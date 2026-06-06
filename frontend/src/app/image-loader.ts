import { IMAGE_CONFIG, IMAGE_LOADER, ImageLoaderConfig } from '@angular/common';
import { Provider } from '@angular/core';
import manifest from '../../public/images/image-manifest.json';

type ManifestEntry = { widths: number[]; hasAlpha: boolean; intrinsic: { w: number; h: number } };
const imageManifest = manifest as Record<string, ManifestEntry>;

export function oasisImageLoader(config: ImageLoaderConfig): string {
  const clean = config.src.replace(/^\/+/, '');
  const withoutImages = clean.replace(/^images\//, '');
  const key = withoutImages.replace(/\.(png|jpe?g|webp)$/i, '');

  const entry = imageManifest[key];

  if (!entry) {
    return `/${clean}`;
  }

  const { widths } = entry;

  if (!config.width) {
    return `/images/optimized/${key}-${widths[widths.length - 1]}.webp`;
  }

  const chosen = widths.find((w) => w >= config.width!) ?? widths[widths.length - 1];
  return `/images/optimized/${key}-${chosen}.webp`;
}

export const imageProviders: Provider[] = [
  { provide: IMAGE_LOADER, useValue: oasisImageLoader },
  {
    provide: IMAGE_CONFIG,
    useValue: {
      breakpoints: [320, 480, 640, 768, 960, 1280, 1600, 1920],
    },
  },
];
