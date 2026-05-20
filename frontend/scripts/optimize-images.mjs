/**
 * scripts/optimize-images.mjs
 *
 * Fase A del pipeline de optimización: convierte imágenes fotográficas/renders
 * de public/images/*.{png,jpg,jpeg} a variantes WebP responsivas y genera el
 * manifest que usará el IMAGE_LOADER en runtime.
 *
 * Req: 2.1, 2.4, 2.5, 2.6, 2.8
 */

import sharp from 'sharp';
import { promises as fs } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
const INPUT_DIR = path.join(ROOT, 'public', 'images');
const OUTPUT_DIR = path.join(ROOT, 'public', 'images', 'optimized');
const MANIFEST_PATH = path.join(ROOT, 'public', 'images', 'image-manifest.json');

// Anchos candidatos (se descarta el upscaling: solo se genera si w <= intrínseco)
const CANDIDATE_WIDTHS = [320, 480, 640, 768, 960, 1280, 1600, 1920];

// Calidad WebP base — se ajusta por imagen si alguna variante supera 400 kB
const BASE_QUALITY = 75;
const MAX_BYTES_PER_VARIANT = 400 * 1024; // 400 kB

// Clasificación: fotografías / renders fotorrealistas a convertir
const PHOTO_PATTERNS = [
  /^.*-ultrareal\.(png|jpe?g)$/i,
  /^.*-hero\.(png|jpe?g)$/i,
  /^club-exterior.*\.(png|jpe?g)$/i,
  /^gallery-\d+\.(png|jpe?g)$/i,
  /^oasis_.*\.(png|jpe?g)$/i,
];

// Exclusiones explícitas (logotipos, iconos, pictogramas)
const EXCLUSION_PATTERNS = [
  /^favicon\.(png|ico)$/i,
];

function isPhoto(filename) {
  if (EXCLUSION_PATTERNS.some((re) => re.test(filename))) return false;
  return PHOTO_PATTERNS.some((re) => re.test(filename));
}

/**
 * Genera una variante WebP para el ancho dado, ajustando la calidad hacia
 * abajo si supera MAX_BYTES_PER_VARIANT. Devuelve null si sharp falla.
 */
async function generateVariant(inputPath, outputPath, width, quality = BASE_QUALITY) {
  const minQuality = 50;
  let q = quality;

  while (q >= minQuality) {
    try {
      const buf = await sharp(inputPath)
        .resize({ width, withoutEnlargement: true, fit: 'inside' })
        .webp({ quality: q })
        .toBuffer();

      if (buf.length <= MAX_BYTES_PER_VARIANT || q === minQuality) {
        await fs.writeFile(outputPath, buf);
        return { bytes: buf.length, quality: q };
      }
      q -= 5;
    } catch (err) {
      console.warn(`  ⚠ sharp failed (q=${q}): ${err.message}`);
      return null;
    }
  }
  return null;
}

async function run() {
  await fs.mkdir(OUTPUT_DIR, { recursive: true });

  const entries = await fs.readdir(INPUT_DIR, { withFileTypes: true });
  const imageFiles = entries
    .filter((e) => e.isFile() && /\.(png|jpe?g)$/i.test(e.name))
    .map((e) => e.name);

  const manifest = {};
  let converted = 0;
  let skipped = 0;
  let failed = 0;

  for (const filename of imageFiles) {
    if (!isPhoto(filename)) {
      skipped++;
      continue;
    }

    const inputPath = path.join(INPUT_DIR, filename);
    const base = filename.replace(/\.(png|jpe?g)$/i, '');

    // Obtener metadatos intrínsecos
    let meta;
    try {
      meta = await sharp(inputPath).metadata();
    } catch (err) {
      console.warn(`⚠ Cannot read metadata for ${filename}: ${err.message}`);
      failed++;
      continue;
    }

    const intrinsicW = meta.width;
    const intrinsicH = meta.height;
    const hasAlpha = meta.hasAlpha ?? false;

    const widths = CANDIDATE_WIDTHS.filter((w) => w <= intrinsicW);
    // Siempre incluir al menos el ancho intrínseco si ningún candidato lo alcanza
    if (widths.length === 0) widths.push(intrinsicW);

    const generatedWidths = [];
    let anyFailed = false;

    console.log(`\n📷 ${filename} (${intrinsicW}×${intrinsicH}, alpha=${hasAlpha})`);

    for (const w of widths) {
      const outFile = path.join(OUTPUT_DIR, `${base}-${w}.webp`);
      const result = await generateVariant(inputPath, outFile, w);

      if (result) {
        const kb = (result.bytes / 1024).toFixed(1);
        const warn = result.bytes > MAX_BYTES_PER_VARIANT ? ' ⚠ OVER BUDGET' : '';
        console.log(`  ✓ ${base}-${w}.webp  ${kb} kB  (q=${result.quality})${warn}`);
        generatedWidths.push(w);
      } else {
        console.warn(`  ✗ Failed to generate ${base}-${w}.webp — skipping this width`);
        anyFailed = true;
      }
    }

    if (generatedWidths.length === 0) {
      console.warn(`⚠ All widths failed for ${filename} — excluded from manifest`);
      failed++;
      continue;
    }

    // Req 2.8: si falla alguna variante pero no todas, se incluyen las exitosas
    manifest[base] = {
      widths: generatedWidths,
      hasAlpha,
      intrinsic: { w: intrinsicW, h: intrinsicH },
    };

    converted++;
    if (anyFailed) failed++;
  }

  await fs.writeFile(MANIFEST_PATH, JSON.stringify(manifest, null, 2), 'utf-8');

  console.log(`\n✅ Pipeline completado:`);
  console.log(`   Convertidas:  ${converted} imágenes`);
  console.log(`   Omitidas:     ${skipped} (no fotográficas/exclusiones)`);
  console.log(`   Con fallos:   ${failed}`);
  console.log(`   Manifest:     ${MANIFEST_PATH}`);
}

run().catch((err) => {
  console.error('❌ Pipeline fatal error:', err);
  process.exit(1);
});
