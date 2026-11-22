# YodhaClip AI — Responsive Web Application

**Long Videos Into Powerful Shorts.**  
A mobile-first React + TypeScript web video editor that processes videos locally in your browser using **FFmpeg.wasm** with zero paid APIs required.

## Features
- **100% Free Local Video Processing:** Upload MP4, MOV, or WebM videos directly in your mobile or desktop browser. Private video files never leave your device.
- **Long Video to Shorts Editor:** Interactive timeline (`IN` / `OUT` sliders + `-1s / +1s` precision nudges), 15s / 30s / 45s / 60s / Custom duration presets, 9:16 / 1:1 / 16:9 framing, Smart Crop modes, and one-tap Auto-Split.
- **Editable Subtitles & Viral Content Kit:** Live animated subtitle overlay preview, clip titles, hashtags, hooks, and `.srt` export in English, Hindi, and Telugu.
- **Real Browser MP4 Export:** Uses single-threaded `@ffmpeg/ffmpeg` (FFmpeg.wasm) so MP4 trimming and 9:16 vertical conversion work directly on **GitHub Pages** without requiring special server headers.
- **Authorized YouTube Link Helper:** Official keyless YouTube oEmbed metadata preview + ownership confirmation + local original video attachment (never bypasses YouTube restrictions).
- **My Projects & My Clips Dashboard:** Persists projects, clips, thumbnails, and local video files in browser `IndexedDB` + `localStorage` so you can resume editing anytime.

## Run Locally
```bash
cd web
npm install
npm run dev
```

## Build for Production
```bash
cd web
npm install
npm run build
```
Static production files are generated in `web/dist/`.

## Free Hosting on GitHub Pages
1. Push this repository to GitHub (branch `main`).
2. Go to **Repository Settings → Pages → Build and deployment → Source** and select **GitHub Actions**.
3. The included workflow `.github/workflows/deploy-pages.yml` automatically builds `web/` and publishes your live website for free.

## Alternative Free Static Hosts (Netlify / Cloudflare Pages / Vercel)
- **Root Directory:** `web`
- **Build Command:** `npm run build`
- **Publish / Output Directory:** `dist`
