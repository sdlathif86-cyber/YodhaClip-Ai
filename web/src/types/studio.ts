export type StudioTab = 'home' | 'editor' | 'projects' | 'clips' | 'settings';

export type AspectRatioOption = '9:16' | '1:1' | '16:9';

export type CaptionStyleOption = 'BOLD_VIOLET' | 'NEON_GLOW' | 'MINIMAL_CLEAN' | 'NONE';

export type SmartCropMode = 'CENTER' | 'SPEAKER_TOP' | 'ACTION_LEFT' | 'ACTION_RIGHT';

export type ExportQualityOption = '720p' | '1080p';

export type SubtitleLanguage = 'en' | 'hi' | 'te';

export interface VideoProject {
  id: string;
  title: string;
  fileName: string;
  formatLabel: string;
  durationSec: number;
  width: number;
  height: number;
  fileSizeBytes: number;
  thumbnailDataUrl: string | null;
  aspectRatio: AspectRatioOption;
  captionStyle: CaptionStyleOption;
  createdAt: number;
  updatedAt: number;
}

export interface ClipItem {
  id: string;
  projectId: string;
  projectTitle: string;
  title: string;
  startSec: number;
  endSec: number;
  aspectRatio: AspectRatioOption;
  captionStyle: CaptionStyleOption;
  smartCropMode: SmartCropMode;
  subtitleText: string;
  language: SubtitleLanguage;
  hashtags: string[];
  thumbnailDataUrl: string | null;
  isFavorite: boolean;
  createdAt: number;
}

export interface ExportHistoryItem {
  id: string;
  fileName: string;
  clipTitle: string;
  aspectRatio: AspectRatioOption;
  quality: ExportQualityOption;
  sizeBytes: number;
  durationSec: number;
  createdAt: number;
}

export interface StudioSettings {
  defaultLanguage: SubtitleLanguage;
  exportQuality: ExportQualityOption;
  defaultClipDurationSec: number;
  defaultAspectRatio: AspectRatioOption;
  defaultCaptionStyle: CaptionStyleOption;
  autoConvert9x16: boolean;
  loopPreviews: boolean;
}

export interface YouTubeOEmbedInfo {
  videoId: string;
  title: string;
  authorName: string;
  thumbnailUrl: string;
  sourceUrl: string;
}

export interface HighlightSuggestion {
  id: string;
  title: string;
  startSec: number;
  endSec: number;
  score: number;
  reason: string;
  subtitleText: string;
  approved: boolean;
}

export const DEFAULT_STUDIO_SETTINGS: StudioSettings = {
  defaultLanguage: 'en',
  exportQuality: '1080p',
  defaultClipDurationSec: 30,
  defaultAspectRatio: '9:16',
  defaultCaptionStyle: 'BOLD_VIOLET',
  autoConvert9x16: true,
  loopPreviews: true,
};

export function formatTimecode(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds));
  const hrs = Math.floor(safe / 3600);
  const mins = Math.floor((safe % 3600) / 60);
  const secs = safe % 60;
  const pad = (n: number) => n.toString().padStart(2, '0');
  return hrs > 0 ? `${hrs}:${pad(mins)}:${pad(secs)}` : `${pad(mins)}:${pad(secs)}`;
}

export function formatFileSize(bytes: number): string {
  if (!bytes || bytes <= 0) return '0 KB';
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}
