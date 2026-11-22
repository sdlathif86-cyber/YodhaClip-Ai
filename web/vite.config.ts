import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Relative base './' ensures zero-config deployment to GitHub Pages,
// Netlify, Cloudflare Pages, or any static web host.
export default defineConfig({
  base: './',
  plugins: [react()],
  optimizeDeps: {
    exclude: ['@ffmpeg/ffmpeg', '@ffmpeg/util'],
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
});
