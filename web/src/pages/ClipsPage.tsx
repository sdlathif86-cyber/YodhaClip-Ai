import React, { useEffect, useMemo, useState } from 'react';
import {
  Download,
  Edit3,
  FileText,
  Film,
  Play,
  Search,
  Star,
  Trash2,
  Upload,
  X,
} from 'lucide-react';
import { VideoPlayer } from '../components/VideoPlayer';
import { getProjectVideoBlob } from '../services/storage';
import { buildSrtContent, sanitizeFileName, triggerBrowserDownload } from '../services/videoProcessor';
import {
  AspectRatioOption,
  CaptionStyleOption,
  ClipItem,
  StudioSettings,
  formatTimecode,
} from '../types/studio';

interface ClipsPageProps {
  clips: ClipItem[];
  settings: StudioSettings;
  exportingClipId: string | null;
  exportProgress: number;
  exportStage: string;
  onCreateNew: () => void;
  onToggleFavorite: (clipId: string) => void;
  onUpdateClip: (
    clipId: string,
    updates: Partial<Pick<ClipItem, 'title' | 'subtitleText' | 'startSec' | 'endSec' | 'aspectRatio' | 'captionStyle'>>
  ) => void;
  onDeleteClip: (clipId: string) => void;
  onExportClipMp4: (clip: ClipItem) => void;
}

type FilterMode = 'ALL' | 'FAVORITES' | '9:16' | '1:1' | '16:9';

export const ClipsPage: React.FC<ClipsPageProps> = ({
  clips,
  settings,
  exportingClipId,
  exportProgress,
  exportStage,
  onCreateNew,
  onToggleFavorite,
  onUpdateClip,
  onDeleteClip,
  onExportClipMp4,
}) => {
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState<FilterMode>('ALL');
  const [activeClipId, setActiveClipId] = useState<string | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [editSubtitle, setEditSubtitle] = useState('');
  const [editRatio, setEditRatio] = useState<AspectRatioOption>('9:16');
  const [editCaptionStyle, setEditCaptionStyle] = useState<CaptionStyleOption>('BOLD_VIOLET');

  const activeClip = useMemo(
    () => clips.find((c) => c.id === activeClipId) ?? null,
    [clips, activeClipId]
  );

  useEffect(() => {
    let revokedUrl: string | null = null;
    if (!activeClip) {
      setPreviewUrl(null);
      return;
    }
    setEditTitle(activeClip.title);
    setEditSubtitle(activeClip.subtitleText);
    setEditRatio(activeClip.aspectRatio);
    setEditCaptionStyle(activeClip.captionStyle);

    void (async () => {
      const blob = await getProjectVideoBlob(activeClip.projectId);
      if (blob) {
        const url = URL.createObjectURL(blob);
        revokedUrl = url;
        setPreviewUrl(url);
      } else {
        setPreviewUrl(null);
      }
    })();

    return () => {
      if (revokedUrl) URL.revokeObjectURL(revokedUrl);
    };
  }, [activeClip?.id]);

  const filteredClips = useMemo(() => {
    const q = query.trim().toLowerCase();
    return clips.filter((clip) => {
      const matchesQuery =
        !q ||
        clip.title.toLowerCase().includes(q) ||
        clip.projectTitle.toLowerCase().includes(q) ||
        clip.subtitleText.toLowerCase().includes(q);
      const matchesFilter =
        filter === 'ALL' ||
        (filter === 'FAVORITES' && clip.isFavorite) ||
        clip.aspectRatio === filter;
      return matchesQuery && matchesFilter;
    });
  }, [clips, query, filter]);

  const handleDownloadSrt = (clip: ClipItem) => {
    const srt = buildSrtContent(clip.subtitleText, Math.max(1, clip.endSec - clip.startSec));
    const blob = new Blob([srt], { type: 'application/x-subrip;charset=utf-8' });
    triggerBrowserDownload(blob, `${sanitizeFileName(clip.title)}_${clip.language}.srt`);
  };

  return (
    <div className="stack">
      <div className="row-between">
        <div>
          <h1 style={{ fontSize: 26 }}>My Clips</h1>
          <p style={{ margin: '4px 0 0', fontSize: 13.5, color: 'var(--text-secondary)' }}>
            Preview, edit captions, and download trimmed MP4 Shorts
          </p>
        </div>
        <button type="button" className="btn btn-primary btn-sm" onClick={onCreateNew}>
          <Upload size={15} />
          Create Clips
        </button>
      </div>

      {clips.length > 0 && (
        <div className="stack" style={{ gap: 10 }}>
          <div style={{ position: 'relative' }}>
            <Search
              size={16}
              color="#847ca3"
              style={{ position: 'absolute', left: 13, top: '50%', transform: 'translateY(-50%)' }}
            />
            <input
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search clips by title, project, or subtitle…"
              className="input"
              style={{ paddingLeft: 38 }}
            />
          </div>

          <div className="chip-row">
            {(['ALL', 'FAVORITES', '9:16', '1:1', '16:9'] as FilterMode[]).map((mode) => (
              <button
                key={mode}
                type="button"
                className={`chip ${filter === mode ? 'selected' : ''}`}
                onClick={() => setFilter(mode)}
              >
                {mode === 'ALL' ? 'All Clips' : mode === 'FAVORITES' ? 'Favorites' : mode}
              </button>
            ))}
          </div>
        </div>
      )}

      {filteredClips.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: 34 }}>
          <Film size={32} color="#a855f7" style={{ marginBottom: 10 }} />
          <div style={{ fontWeight: 700, fontSize: 17 }}>
            {clips.length === 0 ? 'No clips saved yet' : 'No clips match this filter'}
          </div>
          <p style={{ color: 'var(--text-secondary)', fontSize: 13.5, margin: '6px 0 18px' }}>
            Create clips in the Shorts Editor and save them to build your Shorts library.
          </p>
          <button type="button" className="btn btn-primary" onClick={onCreateNew}>
            <Upload size={16} />
            Open Shorts Editor
          </button>
        </div>
      ) : (
        <div className="clips-grid">
          {filteredClips.map((clip) => {
            const isExporting = exportingClipId === clip.id;
            return (
              <article key={clip.id} className="card stack" style={{ gap: 10 }}>
                <div style={{ position: 'relative' }}>
                  {clip.thumbnailDataUrl ? (
                    <img src={clip.thumbnailDataUrl} alt={clip.title} className="thumb-img" />
                  ) : (
                    <div
                      className="thumb-img"
                      style={{ display: 'grid', placeItems: 'center', color: 'var(--text-muted)' }}
                    >
                      <Film size={28} />
                    </div>
                  )}
                  <button
                    type="button"
                    onClick={() => onToggleFavorite(clip.id)}
                    aria-label="Toggle favorite"
                    style={{
                      position: 'absolute',
                      top: 8,
                      right: 8,
                      width: 36,
                      height: 36,
                      borderRadius: 999,
                      border: '1px solid rgba(255,255,255,0.18)',
                      background: 'rgba(7,6,13,0.75)',
                      display: 'grid',
                      placeItems: 'center',
                      cursor: 'pointer',
                    }}
                  >
                    <Star
                      size={16}
                      color={clip.isFavorite ? '#f59e0b' : '#b9b2d3'}
                      fill={clip.isFavorite ? '#f59e0b' : 'none'}
                    />
                  </button>
                </div>

                <div>
                  <h3 style={{ fontSize: 15.5 }}>{clip.title}</h3>
                  <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 2 }}>
                    {clip.projectTitle}
                  </div>
                </div>

                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                  <span className="pill font-mono">
                    {formatTimecode(clip.startSec)} – {formatTimecode(clip.endSec)}
                  </span>
                  <span className="pill pill-muted font-mono">{clip.aspectRatio}</span>
                </div>

                {clip.subtitleText && (
                  <div
                    style={{
                      fontSize: 12.5,
                      color: '#d8b4fe',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    “{clip.subtitleText}”
                  </div>
                )}

                {isExporting && (
                  <div className="stack" style={{ gap: 6 }}>
                    <div className="progress-track">
                      <div className="progress-fill" style={{ width: `${exportProgress}%` }} />
                    </div>
                    <div style={{ fontSize: 11.5, color: 'var(--emerald-ready)' }}>
                      {exportStage || `Exporting MP4… ${exportProgress}%`}
                    </div>
                  </div>
                )}

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr auto auto', gap: 6 }}>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => setActiveClipId(clip.id)}
                  >
                    <Play size={14} />
                    Preview
                  </button>
                  <button
                    type="button"
                    className="btn btn-emerald btn-sm"
                    disabled={exportingClipId !== null}
                    onClick={() => onExportClipMp4(clip)}
                  >
                    <Download size={14} />
                    MP4
                  </button>
                  <button
                    type="button"
                    className="btn btn-ghost btn-sm"
                    title="Download SRT subtitles"
                    onClick={() => handleDownloadSrt(clip)}
                  >
                    <FileText size={14} />
                  </button>
                  <button
                    type="button"
                    className="btn btn-danger btn-sm"
                    aria-label={`Delete ${clip.title}`}
                    onClick={() => onDeleteClip(clip.id)}
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}

      {/* Preview & Inline Edit Modal */}
      {activeClip && (
        <div className="modal-backdrop" onClick={() => setActiveClipId(null)}>
          <div className="modal-card stack" onClick={(e) => e.stopPropagation()}>
            <div className="row-between">
              <h3 style={{ fontSize: 18 }}>Preview & Edit Clip</h3>
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                onClick={() => setActiveClipId(null)}
                aria-label="Close modal"
              >
                <X size={16} />
              </button>
            </div>

            {previewUrl ? (
              <VideoPlayer
                videoUrl={previewUrl}
                durationSec={activeClip.endSec + 1}
                aspectRatio={editRatio}
                captionStyle={editCaptionStyle}
                smartCropMode={activeClip.smartCropMode}
                subtitleText={editSubtitle}
                clipStartSec={activeClip.startSec}
                clipEndSec={activeClip.endSec}
                loop={settings.loopPreviews}
              />
            ) : (
              <div className="alert-error">
                Original local video file was cleared from browser storage. Re-open the video in the
                Shorts Editor to preview or re-export MP4.
              </div>
            )}

            <div>
              <label className="field-label">Clip Title</label>
              <input
                type="text"
                className="input"
                value={editTitle}
                onChange={(e) => setEditTitle(e.target.value)}
              />
            </div>

            <div>
              <label className="field-label">Editable Subtitles</label>
              <textarea
                className="textarea"
                value={editSubtitle}
                onChange={(e) => setEditSubtitle(e.target.value)}
                placeholder="Enter captions for this Short…"
              />
            </div>

            <div>
              <label className="field-label">Aspect Ratio</label>
              <div className="chip-row">
                {(['9:16', '1:1', '16:9'] as AspectRatioOption[]).map((ratio) => (
                  <button
                    key={ratio}
                    type="button"
                    className={`chip ${editRatio === ratio ? 'selected' : ''}`}
                    onClick={() => setEditRatio(ratio)}
                  >
                    {ratio}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="field-label">Caption Style</label>
              <div className="chip-row">
                {(
                  [
                    ['BOLD_VIOLET', 'Bold Violet'],
                    ['NEON_GLOW', 'Neon Glow'],
                    ['MINIMAL_CLEAN', 'Minimal Clean'],
                    ['NONE', 'Off'],
                  ] as [CaptionStyleOption, string][]
                ).map(([styleCode, label]) => (
                  <button
                    key={styleCode}
                    type="button"
                    className={`chip ${editCaptionStyle === styleCode ? 'selected' : ''}`}
                    onClick={() => setEditCaptionStyle(styleCode)}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
              <button
                type="button"
                className="btn btn-primary"
                style={{ flex: 1 }}
                onClick={() => {
                  onUpdateClip(activeClip.id, {
                    title: editTitle.trim() || activeClip.title,
                    subtitleText: editSubtitle,
                    aspectRatio: editRatio,
                    captionStyle: editCaptionStyle,
                  });
                  setActiveClipId(null);
                }}
              >
                <Edit3 size={15} />
                Save Changes
              </button>
              <button
                type="button"
                className="btn btn-emerald"
                disabled={exportingClipId !== null}
                onClick={() => onExportClipMp4(activeClip)}
              >
                <Download size={15} />
                Download MP4
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
