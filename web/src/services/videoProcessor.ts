import { FFmpeg } from '@ffmpeg/ffmpeg';
import { fetchFile, toBlobURL } from '@ffmpeg/util';
import {
  AspectRatioOption,
  ExportQualityOption,
  SmartCropMode,
} from '../types/studio';

export interface LocalVideoMetadata {
  fileName: string;
  formatLabel: string;
  durationSec: number;
  width: number;
  height: number;
  fileSizeBytes: number;
  thumbnailDataUrl: string | null;
}

export interface ExportMp4Options {
  sourceFile: Blob;
  sourceFileName: string;
  clipTitle: string;
  startSec: number;
  endSec: number;
  aspectRatio: AspectRatioOption;
  smartCropMode: SmartCropMode;
  quality: ExportQualityOption;
  onProgress?: (percent: number, stage: string) => void;
}

export interface ExportMp4Result {
  blob: Blob;
  fileName: string;
  sizeBytes: number;
  durationSec: number;
}

// Single-threaded @ffmpeg/core works on GitHub Pages and standard HTTPS static hosts
// without requiring Cross-Origin-Opener-Policy / SharedArrayBuffer headers.
const FFMPEG_CORE_BASE_URL = 'https://unpkg.com/@ffmpeg/core@0.12.6/dist/esm';

let ffmpegInstance: FFmpeg | null = null;
let ffmpegLoadingPromise: Promise<FFmpeg> | null = null;

export function detectFormatLabel(fileName: string, mimeType = ''): string {
  const ext = fileName.split('.').pop()?.toLowerCase() ?? '';
  if (ext === 'mp4' || ext === 'm4v' || mimeType.includes('mp4')) return 'MP4';
  if (ext === 'mov' || ext === 'qt' || mimeType.includes('quicktime')) return 'MOV';
  if (ext === 'webm' || mimeType.includes('webm')) return 'WEBM';
  if (ext === 'mkv') return 'MKV';
  return ext ? ext.toUpperCase() : 'MP4';
}

export async function extractVideoMetadata(file: File): Promise<LocalVideoMetadata> {
  const objectUrl = URL.createObjectURL(file);
  try {
    const video = document.createElement('video');
    video.preload = 'metadata';
    video.muted = true;
    video.playsInline = true;
    video.src = objectUrl;

    await new Promise<void>((resolve, reject) => {
      video.onloadedmetadata = () => resolve();
      video.onerror = () => reject(new Error('Could not decode this video in your browser. Try an MP4, MOV, or WebM file.'));
    });

    const durationSec = Number.isFinite(video.duration) && video.duration > 0 ? video.duration : 1;
    const width = video.videoWidth || 1280;
    const height = video.videoHeight || 720;

    let thumbnailDataUrl: string | null = null;
    try {
      const seekSec = durationSec > 2 ? 1 : durationSec * 0.4;
      video.currentTime = seekSec;
      await new Promise<void>((resolve) => {
        video.onseeked = () => resolve();
        setTimeout(() => resolve(), 1200);
      });
      thumbnailDataUrl = drawVideoFrameDataUrl(video);
    } catch {
      thumbnailDataUrl = null;
    }

    return {
      fileName: file.name,
      formatLabel: detectFormatLabel(file.name, file.type),
      durationSec,
      width,
      height,
      fileSizeBytes: file.size,
      thumbnailDataUrl,
    };
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
}

export async function captureFrameAt(file: Blob, timeSec: number): Promise<string | null> {
  const objectUrl = URL.createObjectURL(file);
  try {
    const video = document.createElement('video');
    video.preload = 'auto';
    video.muted = true;
    video.playsInline = true;
    video.src = objectUrl;

    await new Promise<void>((resolve, reject) => {
      video.onloadedmetadata = () => resolve();
      video.onerror = () => reject(new Error('Frame decode failed'));
    });

    const safeTime = Math.min(Math.max(0, timeSec), Math.max(0.1, (video.duration || 1) - 0.1));
    video.currentTime = safeTime;
    await new Promise<void>((resolve) => {
      video.onseeked = () => resolve();
      setTimeout(() => resolve(), 1200);
    });

    return drawVideoFrameDataUrl(video);
  } catch {
    return null;
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
}

function drawVideoFrameDataUrl(video: HTMLVideoElement): string | null {
  const maxWidth = 420;
  const w = video.videoWidth || 320;
  const h = video.videoHeight || 180;
  const scale = Math.min(1, maxWidth / w);
  const canvas = document.createElement('canvas');
  canvas.width = Math.max(1, Math.round(w * scale));
  canvas.height = Math.max(1, Math.round(h * scale));
  const ctx = canvas.getContext('2d');
  if (!ctx) return null;
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
  return canvas.toDataURL('image/jpeg', 0.78);
}

async function ensureFfmpegLoaded(
  onProgress?: (percent: number, stage: string) => void
): Promise<FFmpeg> {
  if (ffmpegInstance && ffmpegInstance.loaded) {
    return ffmpegInstance;
  }
  if (ffmpegLoadingPromise) {
    return ffmpegLoadingPromise;
  }

  ffmpegLoadingPromise = (async () => {
    onProgress?.(8, 'Loading FFmpeg.wasm local video engine…');
    const ffmpeg = new FFmpeg();
    const coreURL = await toBlobURL(`${FFMPEG_CORE_BASE_URL}/ffmpeg-core.js`, 'text/javascript');
    const wasmURL = await toBlobURL(`${FFMPEG_CORE_BASE_URL}/ffmpeg-core.wasm`, 'application/wasm');
    await ffmpeg.load({ coreURL, wasmURL });
    ffmpegInstance = ffmpeg;
    return ffmpeg;
  })();

  try {
    return await ffmpegLoadingPromise;
  } catch (err) {
    ffmpegLoadingPromise = null;
    throw err;
  }
}

function buildCropScaleFilter(
  aspectRatio: AspectRatioOption,
  cropMode: SmartCropMode,
  quality: ExportQualityOption
): string | null {
  const targetDims =
    aspectRatio === '9:16'
      ? quality === '720p'
        ? { w: 720, h: 1280 }
        : { w: 1080, h: 1920 }
      : aspectRatio === '1:1'
        ? quality === '720p'
          ? { w: 720, h: 720 }
          : { w: 1080, h: 1080 }
        : null;

  if (!targetDims) return null;

  const { w, h } = targetDims;
  const cropX =
    cropMode === 'ACTION_LEFT'
      ? '0'
      : cropMode === 'ACTION_RIGHT'
        ? 'iw-ow'
        : '(iw-ow)/2';
  const cropY = cropMode === 'SPEAKER_TOP' ? '0' : '(ih-oh)/2';

  return `scale=${w}:${h}:force_original_aspect_ratio=increase,crop=${w}:${h}:${cropX}:${cropY},setsar=1`;
}

export function sanitizeFileName(raw: string): string {
  return (
    raw
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '')
      .slice(0, 42) || 'yodhaclip_short'
  );
}

export async function trimAndExportMp4(options: ExportMp4Options): Promise<ExportMp4Result> {
  const {
    sourceFile,
    sourceFileName,
    clipTitle,
    startSec,
    endSec,
    aspectRatio,
    smartCropMode,
    quality,
    onProgress,
  } = options;

  const safeStart = Math.max(0, startSec);
  const clipDuration = Math.max(1, endSec - safeStart);
  const ext = sourceFileName.split('.').pop()?.toLowerCase() || 'mp4';
  const inputName = `input_${Date.now()}.${ext}`;
  const outputName = `output_${Date.now()}.mp4`;

  const ffmpeg = await ensureFfmpegLoaded(onProgress);

  const progressHandler = ({ progress }: { progress: number }) => {
    const pct = Math.min(96, Math.max(15, Math.round(15 + progress * 80)));
    onProgress?.(pct, `Processing locally in browser… ${pct}%`);
  };
  ffmpeg.on('progress', progressHandler);

  try {
    onProgress?.(12, 'Reading local video into browser sandbox…');
    await ffmpeg.writeFile(inputName, await fetchFile(sourceFile));

    const vf = buildCropScaleFilter(aspectRatio, smartCropMode, quality);
    const encodeArgs: string[] = [
      '-ss',
      safeStart.toFixed(2),
      '-i',
      inputName,
      '-t',
      clipDuration.toFixed(2),
    ];

    if (vf) {
      encodeArgs.push(
        '-vf',
        vf,
        '-c:v',
        'libx264',
        '-preset',
        'ultrafast',
        '-crf',
        '24',
        '-pix_fmt',
        'yuv420p',
        '-c:a',
        'aac',
        '-b:a',
        '128k',
        '-movflags',
        '+faststart',
        outputName
      );
    } else {
      encodeArgs.push('-c', 'copy', '-movflags', '+faststart', outputName);
    }

    onProgress?.(18, vf ? `Rendering ${aspectRatio} (${quality}) MP4 locally…` : 'Trimming MP4 locally…');
    const exitCode = await ffmpeg.exec(encodeArgs);

    if (exitCode !== 0) {
      // Fallback to fast local stream copy if hardware/memory limits reject transcoding
      onProgress?.(55, 'Retrying with fast local stream-copy trim…');
      const fallbackCode = await ffmpeg.exec([
        '-ss',
        safeStart.toFixed(2),
        '-i',
        inputName,
        '-t',
        clipDuration.toFixed(2),
        '-c',
        'copy',
        '-movflags',
        '+faststart',
        outputName,
      ]);
      if (fallbackCode !== 0) {
        throw new Error('Browser FFmpeg could not export this clip. Try a shorter duration or standard MP4 source.');
      }
    }

    const data = await ffmpeg.readFile(outputName);
    const bytes = data instanceof Uint8Array ? new Uint8Array(data) : new TextEncoder().encode(String(data));
    const blob = new Blob([bytes.buffer], { type: 'video/mp4' });
    const suffix = aspectRatio === '9:16' ? '9x16' : aspectRatio === '1:1' ? '1x1' : '16x9';
    const fileName = `${sanitizeFileName(clipTitle)}_${suffix}_${quality}.mp4`;

    onProgress?.(100, 'MP4 ready for download');
    return {
      blob,
      fileName,
      sizeBytes: blob.size,
      durationSec: clipDuration,
    };
  } finally {
    ffmpeg.off('progress', progressHandler);
    try {
      await ffmpeg.deleteFile(inputName);
    } catch {
      // ignore
    }
    try {
      await ffmpeg.deleteFile(outputName);
    } catch {
      // ignore
    }
  }
}

export function buildSrtContent(subtitleText: string, durationSec: number): string {
  const lines = subtitleText
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
  const cues = lines.length > 0 ? lines : ['(No subtitles added)'];
  const safeDuration = Math.max(1, durationSec);
  const slice = safeDuration / cues.length;

  const fmt = (sec: number): string => {
    const ms = Math.max(0, Math.round(sec * 1000));
    const h = Math.floor(ms / 3_600_000);
    const m = Math.floor((ms % 3_600_000) / 60_000);
    const s = Math.floor((ms % 60_000) / 1000);
    const rem = ms % 1000;
    const p2 = (n: number) => n.toString().padStart(2, '0');
    const p3 = (n: number) => n.toString().padStart(3, '0');
    return `${p2(h)}:${p2(m)}:${p2(s)},${p3(rem)}`;
  };

  return cues
    .map((cue, idx) => `${idx + 1}\n${fmt(idx * slice)} --> ${fmt((idx + 1) * slice)}\n${cue}\n`)
    .join('\n');
}

export function triggerBrowserDownload(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  setTimeout(() => URL.revokeObjectURL(url), 15_000);
}
