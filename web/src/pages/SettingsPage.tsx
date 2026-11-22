import React from 'react';
import { Download, RotateCcw, ShieldCheck, Trash2 } from 'lucide-react';
import {
  AspectRatioOption,
  CaptionStyleOption,
  ExportHistoryItem,
  ExportQualityOption,
  StudioSettings,
  SubtitleLanguage,
  formatFileSize,
  formatTimecode,
} from '../types/studio';

interface SettingsPageProps {
  settings: StudioSettings;
  exportsHistory: ExportHistoryItem[];
  projectCount: number;
  clipCount: number;
  onUpdateSettings: (patch: Partial<StudioSettings>) => void;
  onResetSettings: () => void;
  onClearAllStorage: () => void;
}

export const SettingsPage: React.FC<SettingsPageProps> = ({
  settings,
  exportsHistory,
  projectCount,
  clipCount,
  onUpdateSettings,
  onResetSettings,
  onClearAllStorage,
}) => {
  return (
    <div className="stack">
      <div>
        <h1 style={{ fontSize: 26 }}>Studio Settings</h1>
        <p style={{ margin: '4px 0 0', fontSize: 13.5, color: 'var(--text-secondary)' }}>
          Configure export quality, default clip duration, caption styling, and local browser privacy
        </p>
      </div>

      {/* Video & Export Defaults */}
      <section className="card stack">
        <h2 style={{ fontSize: 17, color: 'var(--violet-bright)' }}>Video & Export Defaults</h2>

        <div>
          <span className="field-label">Export Video Quality</span>
          <div className="chip-row">
            {(['720p', '1080p'] as ExportQualityOption[]).map((q) => (
              <button
                key={q}
                type="button"
                className={`chip ${settings.exportQuality === q ? 'selected' : ''}`}
                onClick={() => onUpdateSettings({ exportQuality: q })}
              >
                {q === '720p' ? '720p HD (Faster on mobile)' : '1080p Full HD'}
              </button>
            ))}
          </div>
        </div>

        <div>
          <span className="field-label">Default Language (Subtitles & Captions)</span>
          <div className="chip-row">
            {(
              [
                ['en', 'English'],
                ['hi', 'Hindi (हिन्दी)'],
                ['te', 'Telugu (తెలుగు)'],
              ] as [SubtitleLanguage, string][]
            ).map(([code, label]) => (
              <button
                key={code}
                type="button"
                className={`chip ${settings.defaultLanguage === code ? 'selected' : ''}`}
                onClick={() => onUpdateSettings({ defaultLanguage: code })}
              >
                {label}
              </button>
            ))}
          </div>
        </div>

        <div>
          <span className="field-label">Default Clip Duration</span>
          <div className="chip-row">
            {[15, 30, 45, 60].map((sec) => (
              <button
                key={sec}
                type="button"
                className={`chip ${settings.defaultClipDurationSec === sec ? 'selected' : ''}`}
                onClick={() => onUpdateSettings({ defaultClipDurationSec: sec })}
              >
                {sec}s
              </button>
            ))}
          </div>
        </div>

        <div>
          <span className="field-label">Default Aspect Ratio</span>
          <div className="chip-row">
            {(['9:16', '1:1', '16:9'] as AspectRatioOption[]).map((ratio) => (
              <button
                key={ratio}
                type="button"
                className={`chip ${settings.defaultAspectRatio === ratio ? 'selected' : ''}`}
                onClick={() => onUpdateSettings({ defaultAspectRatio: ratio })}
              >
                {ratio}
              </button>
            ))}
          </div>
        </div>

        <div>
          <span className="field-label">Default Caption Style</span>
          <div className="chip-row">
            {(
              [
                ['BOLD_VIOLET', 'Bold Violet'],
                ['NEON_GLOW', 'Neon Glow'],
                ['MINIMAL_CLEAN', 'Minimal Clean'],
                ['NONE', 'Off'],
              ] as [CaptionStyleOption, string][]
            ).map(([code, label]) => (
              <button
                key={code}
                type="button"
                className={`chip ${settings.defaultCaptionStyle === code ? 'selected' : ''}`}
                onClick={() => onUpdateSettings({ defaultCaptionStyle: code })}
              >
                {label}
              </button>
            ))}
          </div>
        </div>

        <label className="row-between" style={{ cursor: 'pointer' }}>
          <div>
            <div style={{ fontWeight: 600, fontSize: 14 }}>Auto 9:16 Vertical Shorts Export</div>
            <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
              Automatically format exported MP4s to 9:16 vertical Shorts
            </div>
          </div>
          <input
            type="checkbox"
            checked={settings.autoConvert9x16}
            onChange={(e) => onUpdateSettings({ autoConvert9x16: e.target.checked })}
            style={{ width: 20, height: 20, accentColor: '#a855f7' }}
          />
        </label>

        <label className="row-between" style={{ cursor: 'pointer' }}>
          <div>
            <div style={{ fontWeight: 600, fontSize: 14 }}>Loop Clip Previews</div>
            <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
              Automatically loop clips inside the preview player
            </div>
          </div>
          <input
            type="checkbox"
            checked={settings.loopPreviews}
            onChange={(e) => onUpdateSettings({ loopPreviews: e.target.checked })}
            style={{ width: 20, height: 20, accentColor: '#a855f7' }}
          />
        </label>
      </section>

      {/* Export History */}
      <section className="card stack">
        <div className="row-between">
          <h2 style={{ fontSize: 17 }}>Recent Local MP4 Exports ({exportsHistory.length})</h2>
          <span className="pill pill-emerald font-mono">
            <Download size={12} /> BROWSER DOWNLOADS
          </span>
        </div>

        {exportsHistory.length === 0 ? (
          <p style={{ margin: 0, fontSize: 13.5, color: 'var(--text-secondary)' }}>
            Exported MP4 clips will be logged here for quick reference.
          </p>
        ) : (
          <div className="stack" style={{ gap: 8 }}>
            {exportsHistory.slice(0, 8).map((item) => (
              <div
                key={item.id}
                className="row-between"
                style={{
                  padding: '10px 12px',
                  borderRadius: 12,
                  background: 'rgba(255,255,255,0.04)',
                }}
              >
                <div>
                  <div style={{ fontWeight: 600, fontSize: 13.5 }}>{item.fileName}</div>
                  <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                    {item.clipTitle} • {item.aspectRatio} • {item.quality}
                  </div>
                </div>
                <span className="pill pill-muted font-mono">
                  {formatTimecode(item.durationSec)} • {formatFileSize(item.sizeBytes)}
                </span>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Privacy & Browser Storage */}
      <section className="card stack">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <ShieldCheck size={20} color="#10b981" />
          <div>
            <div style={{ fontWeight: 700, fontSize: 15 }}>
              100% Browser-Local & Free — Zero Paid APIs
            </div>
            <div style={{ fontSize: 12.5, color: 'var(--text-secondary)' }}>
              Your videos ({projectCount} projects, {clipCount} clips) stay privately inside your
              browser’s IndexedDB and are processed locally with FFmpeg.wasm.
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <button type="button" className="btn btn-ghost btn-sm" onClick={onResetSettings}>
            <RotateCcw size={14} />
            Reset Settings
          </button>
          <button type="button" className="btn btn-danger btn-sm" onClick={onClearAllStorage}>
            <Trash2 size={14} />
            Clear Local Projects & Video Cache
          </button>
        </div>
      </section>
    </div>
  );
};
