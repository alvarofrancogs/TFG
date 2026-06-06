import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
const OPTIMIZED_DIR = path.join(ROOT, 'public', 'images', 'optimized');

const MAX_KB_PER_IMAGE = 400;

async function checkBudget() {
  try {
    const files = await fs.readdir(OPTIMIZED_DIR);
    let totalBytes = 0;
    let anyExceeded = false;

    for (const file of files) {
      if (!file.endsWith('.webp')) continue;
      
      const filePath = path.join(OPTIMIZED_DIR, file);
      const stat = await fs.stat(filePath);
      const kb = stat.size / 1024;
      totalBytes += stat.size;

      if (kb > MAX_KB_PER_IMAGE) {
        console.error(`❌ BUDGET EXCEEDED: ${file} is ${kb.toFixed(2)} KB (Max: ${MAX_KB_PER_IMAGE} KB)`);
        anyExceeded = true;
      }
    }

    const totalMb = totalBytes / (1024 * 1024);
    console.log(`✅ Total optimized images size: ${totalMb.toFixed(2)} MB`);

    if (anyExceeded) {
      console.error('❌ Build failed due to image budget violation.');
      process.exit(1);
    } else {
      console.log('✅ All images within budget.');
    }
  } catch (err) {
    if (err.code === 'ENOENT') {
      console.log('No optimized images directory found. Skipping budget check.');
    } else {
      console.error('Error checking image budget:', err);
      process.exit(1);
    }
  }
}

checkBudget();
