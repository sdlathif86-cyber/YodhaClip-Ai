import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  CheckCircle2,
  Download,
  Edit3,
  FileText,
  Film,
  Link as LinkIcon,
  Play,
  Plus,
  Save,
  Scissors,
  ShieldCheck,
  Sparkles,
  Trash2,
  Upload,
  Wand2,
  X,
} from 'lucide-react';
import { VideoPlayer } from '../components/VideoPlayer';
import { analyzeTranscriptLocally } from '../services/transcriptAnalyzer';
import {
  LocalVideoMetadata,
  buildSrtContent,
  captureFrameAt,
  extractVideoMetadata,
  sanitizeFileName,
  triggerBrowserDownload,
} from '../services/videoProcessor';
import { fetchYouTubeOEmbed } from '../services/youtubeMetadata';
import {
  AspectRatioOption,
  CaptionStyleOption,
  ClipItem,
  HighlightSuggestion,
  SmartCropMode,
  StudioSettings,
  SubtitleLanguage,
  VideoProject,
  YouTubeOEmbedInfo,
  formatFileSize,
  formatTimecode,
} from '../types/studio';

export interface ActiveEditorSession {
  projectId: string | null;
  projectTitle: string;
  sourceFile: Blob;
  metadata: LocalVideoMetadata;
  stagedClips: ClipItem[];
}

interface ShortsEditorPageProps {
  settings: StudioSettings;
  activeSession: ActiveEditorSession | null;
  exportingClipId: string | null;
  exportProgress: number;
  exportStage: string;
  onSessionChange: (session: ActiveEditorSession | null) => void;
  onSaveProjectAndClips: (
    session: ActiveEditorSession,
    aspectRatio: AspectRatioOption,
    captionStyle: CaptionStyleOption
  ) => Promise<VideoProject>;
  onExportClipMp4: (clip: ClipItem, sourceFile: Blob, sourceFileName: string) => Promise<void>;
  onExportAllMp4: (clips: ClipItem[], sourceFile: Blob, sourceFileName: string) => Promise<void>;
  onNotify: (message: string) => void;
}

type InputTab = 'upload' | 'youtube';

export const ShortsEditorPage: React.FC<ShortsEditorPageProps> = ({
  settings,
  activeSession,
  exportingClipId,
  exportProgress,
  exportStage,
  onSessionChange,
  onSaveProjectAndClips,
  onExportClipMp4,
  onExportAllMp4,
  onNotify,
}) => {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const transcriptFileInputRef = useRef<HTMLInputElement | null>(null);

  const [inputTab, setInputTab] = useState<InputTab>('upload');
  const [isInspecting, setIsInspecting] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);

  // Authorized YouTube metadata state
  const [ytUrl, setYtUrl] = useState('');
  const [ytInfo, setYtInfo] = useState<YouTubeOEmbedInfo | null>(null);
  const [ytLoading, setYtLoading] = useState(false);
  const [ytError, setYtError] = useState<string | null>(null);
  const [ytOwnedConfirmed, setYtOwnedConfirmed] = useState(false);

  // Timeline & Clip Editor state
  const [aspectRatio, setAspectRatio] = useState<AspectRatioOption>(settings.defaultAspectRatio);
  const [captionStyle, setCaptionStyle] = useState<CaptionStyleOption>(settings.defaultCaptionStyle);
  const [smartCrop, setSmartCrop] = useState<SmartCropMode>('CENTER');
  const [language, setLanguage] = useState<SubtitleLanguage>(settings.defaultLanguage);
  const [clipDurationPreset, setClipDurationPreset] = useState<number>(
    settings.defaultClipDurationSec
  );

  const [trimStartSec, setTrimStartSec] = useState(0);
  const [trimEndSec, setTrimEndSec] = useState(30);
  const [clipTitle, setClipTitle] = useState('Highlight #1');
  const [clipSubtitle, setClipSubtitle] = useState('');
  const [seekTriggerSec, setSeekTriggerSec] = useState<number | null>(null);
  const [previewingClipId, setPreviewingClipId] = useState<string | null>(null);

  // Inline Edit Modal state for staged clips
  const [editingClipId, setEditingClipId] = useState<string | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [editSubtitle, setEditSubtitle] = useState('');
  const [editStartSec, setEditStartSec] = useState(0);
  const [editEndSec, setEditEndSec] = useState(15);

  // Local Transcript Highlight Analyzer (100% Free, Zero Paid APIs)
  const [transcriptText, setTranscriptText] = useState('');
  const [suggestions, setSuggestions] = useState<HighlightSuggestion[]>([]);
  const [analyzerMessage, setAnalyzerMessage] = useState<string | null>(null);

  const [isSaving, setIsSaving] = useState(false);

  // Object URL for active local video file
  const videoUrl = useMemo(() => {
    if (!activeSession) return null;
    return URL.createObjectURL(activeSession.sourceFile);
  }, [activeSession?.sourceFile]);

  useEffect(() => {
    return () => {
      if (videoUrl) URL.revokeObjectURL(videoUrl);
    };
  }, [videoUrl]);

  // Reset trim window when a new video loads
  useEffect(() => {
    if (!activeSession) return;
    const dur = Math.max(1, activeSession.metadata.durationSec);
    setTrimStartSec(0);
    setTrimEndSec(Math.min(dur, Math.max(3, settings.defaultClipDurationSec)));
    setClipTitle(`Highlight #${activeSession.stagedClips.length + 1}`);
    setPreviewingClipId(null);
  }, [activeSession?.sourceFile, settings.defaultClipDurationSec]);

  const activePreviewClip = useMemo(
    () => activeSession?.stagedClips.find((c) => c.id === previewingClipId) ?? null,
    [activeSession?.stagedClips, previewingClipId]
  );

  const handleFileSelected = async (file: File | undefined) => {
    if (!file) return;
    setUploadError(null);
    setIsInspecting(true);
    try {
      const metadata = await extractVideoMetadata(file);
      const suggestedTitle =
        ytInfo?.title ||
        file.name
          .replace(/\.[^.]+$/, '')
          .replace(/[_-]+/g, ' ')
          .trim() ||
        'Untitled Shorts Project';

      onSessionChange({
        projectId: null,
        projectTitle: suggestedTitle,
        sourceFile: file,
        metadata,
        stagedClips: [],
      });
      onNotify(`Loaded ${metadata.fileName} (${formatTimecode(metadata.durationSec)}) locally`);
    } catch (err) {
      setUploadError(
        err instanceof Error
          ? err.message
          : 'Could not read that video file. Please pick an MP4, MOV, or WebM video.'
      );
    } finally {
      setIsInspecting(false);
    }
  };

  const handleFetchYouTube = async () => {
    setYtError(null);
    setYtLoading(true);
    try {
      const info = await fetchYouTubeOEmbed(ytUrl);
      setYtInfo(info);
      onNotify(`Fetched public details for "${info.title}"`);
    } catch (err) {
      setYtInfo(null);
      setYtError(err instanceof Error ? err.message : 'Unable to fetch YouTube video details.');
    } finally {
      setYtLoading(false);
    }
  };

  const applyDurationPreset = (seconds: number) => {
    setClipDurationPreset(seconds);
    if (!activeSession) return;
    const maxDur = activeSession.metadata.durationSec;
    const nextEnd = Math.min(maxDur, trimStartSec + seconds);
    setTrimEndSec(Math.max(trimStartSec + 1, nextEnd));
  };

  const handleAddManualClip = async () => {
    if (!activeSession) return;
    const safeStart = Math.max(0, Math.min(trimStartSec, activeSession.metadata.durationSec - 1));
    const safeEnd = Math.max(safeStart + 1, Math.min(trimEndSec, activeSession.metadata.durationSec));
    const mid = (safeStart + safeEnd) / 2;
    const thumb =
      (await captureFrameAt(activeSession.sourceFile, mid)) ??
      activeSession.metadata.thumbnailDataUrl;

    const newClip: ClipItem = {
      id: `clip_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
      projectId: activeSession.projectId ?? 'draft',
      projectTitle: activeSession.projectTitle,
      title: clipTitle.trim() || `Highlight #${activeSession.stagedClips.length + 1}`,
      startSec: Number(safeStart.toFixed(1)),
      endSec: Number(safeEnd.toFixed(1)),
      aspectRatio,
      captionStyle,
      smartCropMode: smartCrop,
      subtitleText: clipSubtitle.trim(),
      language,
      hashtags: ['#Shorts', '#YodhaClipAI'],
      thumbnailDataUrl: thumb,
      isFavorite: false,
      createdAt: Date.now(),
    };

    onSessionChange({
      ...activeSession,
      stagedClips: [...activeSession.stagedClips, newClip],
    });
    setClipTitle(`Highlight #${activeSession.stagedClips.length + 2}`);
    setClipSubtitle('');
    onNotify(`Added "${newClip.title}" to clips queue`);
  };

  const handleAutoSplit = async () => {
    if (!activeSession) return;
    const total = activeSession.metadata.durationSec;
    const segLen = Math.max(10, clipDurationPreset);
    const count = Math.min(8, Math.max(1, Math.ceil(total / segLen)));
    const step = total / count;
    const labels = [
      'Opening Hook',
      'Key Moment',
      'Highlight Cut',
      'Action Peak',
      'Story Beat',
      'Top Takeaway',
      'Big Reveal',
      'Closing Punch',
    ];

    const created: ClipItem[] = [];
    for (let i = 0; i < count; i++) {
      const s = Number((i * step).toFixed(1));
      const e = Number(Math.min(total, (i + 1) * step).toFixed(1));
      const thumb =
        (await captureFrameAt(activeSession.sourceFile, (s + e) / 2)) ??
        activeSession.metadata.thumbnailDataUrl;
      created.push({
        id: `clip_${Date.now()}_${i}`,
        projectId: activeSession.projectId ?? 'draft',
        projectTitle: activeSession.projectTitle,
        title: `${labels[i % labels.length]} #${i + 1}`,
        startSec: s,
        endSec: Math.max(s + 1, e),
        aspectRatio,
        captionStyle,
        smartCropMode: smartCrop,
        subtitleText: '',
        language,
        hashtags: ['#Shorts', '#YodhaClipAI'],
        thumbnailDataUrl: thumb,
        isFavorite: false,
        createdAt: Date.now() + i,
      });
    }

    onSessionChange({
      ...activeSession,
      stagedClips: created,
    });
    onNotify(`Auto-split video into ${created.length} vertical Shorts clips locally`);
  };

  const handleRunTranscriptAnalyzer = () => {
    if (!activeSession) return;
    setAnalyzerMessage(null);
    if (transcriptText.trim().length < 15) {
      setSuggestions([]);
      setAnalyzerMessage(
        'Paste a transcript (or upload a .txt / .srt file) with at least 15 characters to suggest timestamped highlights locally.'
      );
      return;
    }

    const found = analyzeTranscriptLocally(
      transcriptText,
      activeSession.metadata.durationSec,
      clipDurationPreset,
      6
    );
    if (found.length === 0) {
      setSuggestions([]);
      setAnalyzerMessage(
        'No clear timestamped cues found. Try adding timestamps like [00:15] or full sentences.'
      );
    } else {
      setSuggestions(found);
      onNotify(`Found ${found.length} transcript highlight suggestions — review & approve below`);
    }
  };

  const handleApproveSuggestion = async (item: HighlightSuggestion) => {
    if (!activeSession || item.approved) return;
    const mid = (item.startSec + item.endSec) / 2;
    const thumb =
      (await captureFrameAt(activeSession.sourceFile, mid)) ??
      activeSession.metadata.thumbnailDataUrl;

    const approvedClip: ClipItem = {
      id: `clip_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
      projectId: activeSession.projectId ?? 'draft',
      projectTitle: activeSession.projectTitle,
      title: item.title,
      startSec: item.startSec,
      endSec: item.endSec,
      aspectRatio,
      captionStyle,
      smartCropMode: smartCrop,
      subtitleText: item.subtitleText,
      language,
      hashtags: ['#Shorts', '#YodhaClipAI'],
      thumbnailDataUrl: thumb,
      isFavorite: false,
      createdAt: Date.now(),
    };

    onSessionChange({
      ...activeSession,
      stagedClips: [...activeSession.stagedClips, approvedClip],
    });
    setSuggestions((prev) =>
      prev.map((s) => (s.id === item.id ? { ...s, approved: true } : s))
    );
    onNotify(`Approved "${item.title}" and added to clips queue`);
  };

  const handleDismissSuggestion = (id: string) => {
    setSuggestions((prev) => prev.filter((s) => s.id !== id));
  };

  const handleRemoveStagedClip = (clipId: string) => {
    if (!activeSession) return;
    if (previewingClipId === clipId) setPreviewingClipId(null);
    onSessionChange({
      ...activeSession,
      stagedClips: activeSession.stagedClips.filter((c) => c.id !== clipId),
    });
  };

  const openEditClipModal = (clip: ClipItem) => {
    setEditingClipId(clip.id);
    setEditTitle(clip.title);
    setEditSubtitle(clip.subtitleText);
    setEditStartSec(clip.startSec);
    setEditEndSec(clip.endSec);
  };

  const saveEditClipModal = () => {
    if (!activeSession || !editingClipId) return;
    const maxDur = activeSession.metadata.durationSec;
    const safeStart = Math.max(0, Math.min(editStartSec, maxDur - 1));
    const safeEnd = Math.max(safeStart + 1, Math.min(editEndSec, maxDur));

    onSessionChange({
      ...activeSession,
      stagedClips: activeSession.stagedClips.map((c) =>
        c.id === editingClipId
          ? {
              ...c,
              title: editTitle.trim() || c.title,
              subtitleText: editSubtitle,
              startSec: Number(safeStart.toFixed(1)),
              endSec: Number(safeEnd.toFixed(1)),
            }
          : c
      ),
    });
    setEditingClipId(null);
    onNotify('Updated clip details & subtitles');
  };

  const handleSaveWorkspace = async () => {
    if (!activeSession || isSaving) return;
    setIsSaving(true);
    try {
      let sessionToSave = activeSession;
      if (sessionToSave.stagedClips.length === 0) {
        const mid = (trimStartSec + trimEndSec) / 2;
        const thumb =
          (await captureFrameAt(sessionToSave.sourceFile, mid)) ??
          sessionToSave.metadata.thumbnailDataUrl;
        sessionToSave = {
          ...sessionToSave,
          stagedClips: [
            {
              id: `clip_${Date.now()}`,
              projectId: sessionToSave.projectId ?? 'draft',
              projectTitle: sessionToSave.projectTitle,
              title: clipTitle.trim() || `${sessionToSave.projectTitle} — Clip 1`,
              startSec: Number(trimStartSec.toFixed(1)),
              endSec: Number(trimEndSec.toFixed(1)),
              aspectRatio,
              captionStyle,
              smartCropMode: smartCrop,
              subtitleText: clipSubtitle.trim(),
              language,
              hashtags: ['#Shorts', '#YodhaClipAI'],
              thumbnailDataUrl: thumb,
              isFavorite: false,
              createdAt: Date.now(),
            },
          ],
        };
      }
      await onSaveProjectAndClips(sessionToSave, aspectRatio, captionStyle);
    } finally {
      setIsSaving(false);
    }
  };

  const handleDownloadSrt = (clip: ClipItem) => {
    const srt = buildSrtContent(clip.subtitleText, Math.max(1, clip.endSec - clip.startSec));
    const blob = new Blob([srt], { type: 'application/x-subrip;charset=utf-8' });
    triggerBrowserDownload(blob, `${sanitizeFileName(clip.title)}_${clip.language}.srt`);
  };

  return (
    <div className="stack">
      <div className="row-between">
        <div>
          <h1 style={{ fontSize: 26 }}>Long Video to Shorts Editor</h1>
          <p style={{ margin: '4px 0 0', fontSize: 13.5, color: 'var(--text-secondary)' }}>
            100% browser-local MP4, MOV & WebM trimming, 9:16 vertical crop, subtitles & MP4 export
          </p>
        </div>
        {activeSession && (
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            onClick={() => {
              onSessionChange(null);
              setSuggestions([]);
              setPreviewingClipId(null);
            }}
          >
            <X size={15} />
            New Video
          </button>
        )}
      </div>

      {/* Hidden File Inputs */}
      <input
        ref={fileInputRef}
        type="file"
        accept="video/mp4,video/quicktime,video/webm,video/*"
        style={{ display: 'none' }}
        onChange={(e) => {
          void handleFileSelected(e.target.files?.[0]);
          e.target.value = '';
        }}
      />
      <input
        ref={transcriptFileInputRef}
        type="file"
        accept=".txt,.srt,.vtt,text/plain"
        style={{ display: 'none' }}
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (!file) return;
          void file.text().then((content) => {
            setTranscriptText(content.slice(0, 45_000));
            onNotify(`Loaded transcript file "${file.name}"`);
          });
          e.target.value = '';
        }}
      />

      {uploadError && <div className="alert-error">{uploadError}</div>}

      {isInspecting && (
        <div className="card" style={{ textAlign: 'center', padding: 34 }}>
          <Sparkles size={26} color="#d946ef" style={{ marginBottom: 8 }} />
          <div style={{ fontWeight: 700 }}>Inspecting video locally in your browser…</div>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 6 }}>
            Reading resolution, duration, and extracting preview frame without uploading anything.
          </p>
        </div>
      )}

      {!activeSession && !isInspecting && (
        <div className="stack">
          {/* Input Mode Selector */}
          <div className="chip-row">
            <button
              type="button"
              className={`chip ${inputTab === 'upload' ? 'selected' : ''}`}
              onClick={() => setInputTab('upload')}
            >
              Upload Video (MP4 / MOV / WebM)
            </button>
            <button
              type="button"
              className={`chip ${inputTab === 'youtube' ? 'selected' : ''}`}
              onClick={() => setInputTab('youtube')}
            >
              Authorized YouTube Link
            </button>
          </div>

          {inputTab === 'youtube' && (
            <section className="card stack">
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <LinkIcon size={18} color="#d946ef" />
                <h2 style={{ fontSize: 17 }}>Authorized YouTube Video Reference</h2>
              </div>
              <p style={{ margin: 0, fontSize: 13, color: 'var(--text-secondary)' }}>
                Fetch official public title & thumbnail via keyless YouTube oEmbed, confirm
                ownership/permission, then attach your own original video file below. We never
                bypass YouTube restrictions or download restricted streams.
              </p>

              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                <input
                  type="url"
                  className="input"
                  style={{ flex: 1, minWidth: 220 }}
                  placeholder="https://www.youtube.com/watch?v=..."
                  value={ytUrl}
                  onChange={(e) => setYtUrl(e.target.value)}
                />
                <button
                  type="button"
                  className="btn btn-secondary"
                  disabled={ytLoading || !ytUrl.trim()}
                  onClick={() => void handleFetchYouTube()}
                >
                  {ytLoading ? 'Fetching…' : 'Fetch Video Details'}
                </button>
              </div>

              {ytError && <div className="alert-error">{ytError}</div>}

              {ytInfo && (
                <div
                  className="card"
                  style={{ padding: 12, background: 'rgba(124, 58, 237, 0.12)' }}
                >
                  <div style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
                    <img
                      src={ytInfo.thumbnailUrl}
                      alt={ytInfo.title}
                      style={{
                        width: 112,
                        height: 64,
                        objectFit: 'cover',
                        borderRadius: 10,
                      }}
                    />
                    <div style={{ flex: 1, minWidth: 180 }}>
                      <div style={{ fontWeight: 700, fontSize: 14.5 }}>{ytInfo.title}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 2 }}>
                        Channel: {ytInfo.authorName}
                      </div>
                    </div>
                  </div>
                </div>
              )}

              <label style={{ display: 'flex', alignItems: 'flex-start', gap: 10, cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={ytOwnedConfirmed}
                  onChange={(e) => setYtOwnedConfirmed(e.target.checked)}
                  style={{ marginTop: 3, width: 18, height: 18, accentColor: '#a855f7' }}
                />
                <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
                  I own this video or have explicit permission to edit it, and I will attach my
                  original local video file below.
                </span>
              </label>
            </section>
          )}

          {/* Local Upload Dropzone */}
          <div
            className="dropzone stack"
            style={{ alignItems: 'center', gap: 12 }}
            onClick={() => {
              if (inputTab === 'youtube' && !ytOwnedConfirmed) {
                onNotify('Please confirm video ownership/permission first');
                return;
              }
              fileInputRef.current?.click();
            }}
            onDragOver={(e) => e.preventDefault()}
            onDrop={(e) => {
              e.preventDefault();
              if (inputTab === 'youtube' && !ytOwnedConfirmed) {
                onNotify('Please confirm video ownership/permission first');
                return;
              }
              void handleFileSelected(e.dataTransfer.files?.[0]);
            }}
          >
            <div className="brand-mark" style={{ width: 58, height: 58, borderRadius: 18 }}>
              <Upload size={26} />
            </div>
            <span className="pill pill-emerald font-mono">
              <ShieldCheck size={13} /> MP4 • MOV • WEBM • 100% BROWSER LOCAL
            </span>
            <h2 style={{ fontSize: 22 }}>Tap or Drop Video Here</h2>
            <p style={{ margin: 0, maxWidth: 460, color: 'var(--text-secondary)', fontSize: 14 }}>
              Select a long video from your Android gallery or desktop file manager. Your video
              never leaves your device — trimming and MP4 export happen privately in your browser.
            </p>
            <button
              type="button"
              className="btn btn-primary"
              disabled={inputTab === 'youtube' && !ytOwnedConfirmed}
              onClick={(e) => {
                e.stopPropagation();
                fileInputRef.current?.click();
              }}
            >
              <Film size={17} />
              Choose Video File (MP4 / MOV / WebM)
            </button>
          </div>
        </div>
      )}

      {activeSession && videoUrl && (
        <div className="editor-layout">
          {/* LEFT PANE: Sticky Video Preview & Technical Specs */}
          <div className="editor-sticky-pane stack">
            {activePreviewClip && (
              <div
                className="card row-between"
                style={{
                  padding: '10px 14px',
                  borderColor: 'var(--border-bright)',
                  background: 'rgba(124, 58, 237, 0.2)',
                }}
              >
                <div>
                  <div style={{ fontWeight: 700, fontSize: 13.5 }}>
                    Previewing Clip: {activePreviewClip.title}
                  </div>
                  <div className="font-mono" style={{ fontSize: 12, color: '#e9d5ff' }}>
                    {formatTimecode(activePreviewClip.startSec)} –{' '}
                    {formatTimecode(activePreviewClip.endSec)} ({activePreviewClip.aspectRatio})
                  </div>
                </div>
                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  onClick={() => setPreviewingClipId(null)}
                >
                  Full Video
                </button>
              </div>
            )}

            <VideoPlayer
              videoUrl={videoUrl}
              durationSec={activeSession.metadata.durationSec}
              aspectRatio={activePreviewClip?.aspectRatio ?? aspectRatio}
              captionStyle={activePreviewClip?.captionStyle ?? captionStyle}
              smartCropMode={activePreviewClip?.smartCropMode ?? smartCrop}
              subtitleText={activePreviewClip?.subtitleText ?? clipSubtitle}
              clipStartSec={activePreviewClip?.startSec ?? 0}
              clipEndSec={activePreviewClip?.endSec ?? activeSession.metadata.durationSec}
              loop={settings.loopPreviews}
              seekToSec={seekTriggerSec}
              onSeekConsumed={() => setSeekTriggerSec(null)}
            />

            <div className="card stack" style={{ gap: 10 }}>
              <div className="row-between">
                <div style={{ fontWeight: 700, fontSize: 14 }}>{activeSession.metadata.fileName}</div>
                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  onClick={() => fileInputRef.current?.click()}
                >
                  Swap Video
                </button>
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                <span className="pill font-mono">{activeSession.metadata.formatLabel}</span>
                <span className="pill pill-muted font-mono">
                  {formatTimecode(activeSession.metadata.durationSec)}
                </span>
                <span className="pill pill-muted font-mono">
                  {activeSession.metadata.width}×{activeSession.metadata.height}
                </span>
                <span className="pill pill-muted font-mono">
                  {formatFileSize(activeSession.metadata.fileSizeBytes)}
                </span>
                <span className="pill pill-emerald font-mono">LOCAL ONLY</span>
              </div>
            </div>
          </div>

          {/* RIGHT PANE: Project Settings, Timeline, Subtitles, Transcript Analyzer & Queue */}
          <div className="stack">
            {/* Project Details & Format */}
            <section className="card stack">
              <div>
                <label className="field-label">Project Title</label>
                <input
                  type="text"
                  className="input"
                  value={activeSession.projectTitle}
                  onChange={(e) =>
                    onSessionChange({ ...activeSession, projectTitle: e.target.value })
                  }
                  placeholder="My Shorts Project"
                />
              </div>

              <div>
                <span className="field-label">Shorts Aspect Ratio</span>
                <div className="chip-row">
                  {(
                    [
                      ['9:16', '9:16 Vertical Shorts'],
                      ['1:1', '1:1 Square'],
                      ['16:9', '16:9 Landscape'],
                    ] as [AspectRatioOption, string][]
                  ).map(([ratio, label]) => (
                    <button
                      key={ratio}
                      type="button"
                      className={`chip ${aspectRatio === ratio ? 'selected' : ''}`}
                      onClick={() => setAspectRatio(ratio)}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <span className="field-label">Smart Subject Crop</span>
                <div className="chip-row">
                  {(
                    [
                      ['CENTER', 'Center Focus'],
                      ['SPEAKER_TOP', 'Speaker / Face'],
                      ['ACTION_LEFT', 'Left Action'],
                      ['ACTION_RIGHT', 'Right Action'],
                    ] as [SmartCropMode, string][]
                  ).map(([mode, label]) => (
                    <button
                      key={mode}
                      type="button"
                      className={`chip ${smartCrop === mode ? 'selected' : ''}`}
                      onClick={() => setSmartCrop(mode)}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <span className="field-label">Caption Overlay Style & Language</span>
                <div className="chip-row" style={{ marginBottom: 8 }}>
                  {(
                    [
                      ['BOLD_VIOLET', 'Bold Violet'],
                      ['NEON_GLOW', 'Neon Glow'],
                      ['MINIMAL_CLEAN', 'Minimal Clean'],
                      ['NONE', 'Off'],
                    ] as [CaptionStyleOption, string][]
                  ).map(([style, label]) => (
                    <button
                      key={style}
                      type="button"
                      className={`chip ${captionStyle === style ? 'selected' : ''}`}
                      onClick={() => setCaptionStyle(style)}
                    >
                      {label}
                    </button>
                  ))}
                </div>
                <div className="chip-row">
                  {(
                    [
                      ['en', 'English'],
                      ['hi', 'Hindi'],
                      ['te', 'Telugu'],
                    ] as [SubtitleLanguage, string][]
                  ).map(([code, label]) => (
                    <button
                      key={code}
                      type="button"
                      className={`chip ${language === code ? 'selected' : ''}`}
                      onClick={() => setLanguage(code)}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              </div>
            </section>

            {/* Interactive Timeline & Clip Builder */}
            <section className="card card-accent stack">
              <div className="row-between">
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Scissors size={18} color="#d946ef" />
                  <h2 style={{ fontSize: 17 }}>Timeline & Clip Builder</h2>
                </div>
                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  onClick={() => {
                    setPreviewingClipId(null);
                    setSeekTriggerSec(trimStartSec);
                  }}
                >
                  <Play size={14} />
                  Preview Start
                </button>
              </div>

              {/* Clip Duration Presets */}
              <div>
                <span className="field-label">Quick Clip Length</span>
                <div className="chip-row">
                  {[15, 30, 45, 60].map((sec) => (
                    <button
                      key={sec}
                      type="button"
                      className={`chip ${clipDurationPreset === sec ? 'selected' : ''}`}
                      onClick={() => applyDurationPreset(sec)}
                    >
                      {sec}s
                    </button>
                  ))}
                </div>
              </div>

              <div className="row-between">
                <span className="pill font-mono">IN {formatTimecode(trimStartSec)}</span>
                <span className="pill pill-amber font-mono">
                  LEN {formatTimecode(Math.max(0, trimEndSec - trimStartSec))}
                </span>
                <span className="pill font-mono">OUT {formatTimecode(trimEndSec)}</span>
              </div>

              <div className="stack" style={{ gap: 8 }}>
                <div>
                  <label className="field-label" style={{ marginBottom: 2 }}>
                    Start Timestamp ({formatTimecode(trimStartSec)})
                  </label>
                  <input
                    type="range"
                    min={0}
                    max={Math.max(1, activeSession.metadata.durationSec - 1)}
                    step={0.5}
                    value={trimStartSec}
                    onChange={(e) => {
                      const next = Number.parseFloat(e.target.value);
                      setTrimStartSec(next);
                      if (next >= trimEndSec - 1) {
                        setTrimEndSec(Math.min(activeSession.metadata.durationSec, next + 1));
                      }
                    }}
                    className="timeline-range"
                  />
                </div>
                <div>
                  <label className="field-label" style={{ marginBottom: 2 }}>
                    End Timestamp ({formatTimecode(trimEndSec)})
                  </label>
                  <input
                    type="range"
                    min={Math.min(activeSession.metadata.durationSec, trimStartSec + 1)}
                    max={Math.max(1, activeSession.metadata.durationSec)}
                    step={0.5}
                    value={trimEndSec}
                    onChange={(e) => setTrimEndSec(Number.parseFloat(e.target.value))}
                    className="timeline-range"
                  />
                </div>
              </div>

              {/* Precision -1s / +1s nudges */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 6 }}>
                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  onClick={() => setTrimStartSec((s) => Math.max(0, s - 1))}
                >
                  IN -1s
                </button>
                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  onClick={() => setTrimStartSec((s) => Math.min(trimEndSec - 1, s + 1))}
                >
                  IN +1s
                </button>
                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  onClick={() => setTrimEndSec((e) => Math.max(trimStartSec + 1, e - 1))}
                >
                  OUT -1s
                </button>
                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  onClick={() =>
                    setTrimEndSec((e) => Math.min(activeSession.metadata.durationSec, e + 1))
                  }
                >
                  OUT +1s
                </button>
              </div>

              <div>
                <label className="field-label">Clip Title</label>
                <input
                  type="text"
                  className="input"
                  value={clipTitle}
                  onChange={(e) => setClipTitle(e.target.value)}
                  placeholder="Viral Hook #1"
                />
              </div>

              <div>
                <label className="field-label">Editable Subtitles (Live Overlay)</label>
                <textarea
                  className="textarea"
                  value={clipSubtitle}
                  onChange={(e) => setClipSubtitle(e.target.value)}
                  placeholder="Add subtitle lines for this Short…"
                />
              </div>

              <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                <button
                  type="button"
                  className="btn btn-primary"
                  style={{ flex: 1 }}
                  onClick={() => void handleAddManualClip()}
                >
                  <Plus size={16} />
                  Add Clip to Queue
                </button>
                <button
                  type="button"
                  className="btn btn-secondary"
                  style={{ flex: 1 }}
                  onClick={() => void handleAutoSplit()}
                >
                  <Wand2 size={16} />
                  Auto-Split ({clipDurationPreset}s)
                </button>
              </div>
            </section>

            {/* Free On-Device Transcript Highlight Analyzer */}
            <section className="card stack">
              <div className="row-between">
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Sparkles size={18} color="#a855f7" />
                  <h2 style={{ fontSize: 17 }}>Smart Transcript Highlights (Free Local)</h2>
                </div>
                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  onClick={() => transcriptFileInputRef.current?.click()}
                >
                  <FileText size={14} />
                  Load .SRT / .TXT
                </button>
              </div>

              <p style={{ margin: 0, fontSize: 12.5, color: 'var(--text-secondary)' }}>
                Paste your transcript or load an .srt file to detect engaging hooks and timestamps
                locally in your browser (zero paid APIs). Review and approve suggestions before
                exporting.
              </p>

              <textarea
                className="textarea"
                value={transcriptText}
                onChange={(e) => setTranscriptText(e.target.value)}
                placeholder="Paste transcript e.g. [00:15] Wait until you see the biggest mistake creators make..."
              />

              <button
                type="button"
                className="btn btn-secondary"
                onClick={handleRunTranscriptAnalyzer}
              >
                <Sparkles size={15} />
                Analyze Transcript Locally
              </button>

              {analyzerMessage && <div className="alert-error">{analyzerMessage}</div>}

              {suggestions.length > 0 && (
                <div className="stack" style={{ gap: 10 }}>
                  <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--violet-bright)' }}>
                    Review & Approve Suggested Moments ({suggestions.length})
                  </div>
                  {suggestions.map((item) => (
                    <div
                      key={item.id}
                      className="card stack"
                      style={{
                        padding: 12,
                        gap: 8,
                        background: 'rgba(124, 58, 237, 0.1)',
                      }}
                    >
                      <div className="row-between">
                        <div style={{ fontWeight: 700, fontSize: 14 }}>{item.title}</div>
                        <span className="pill pill-amber font-mono">{item.score}% Score</span>
                      </div>
                      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                        <span className="pill font-mono">
                          {formatTimecode(item.startSec)} – {formatTimecode(item.endSec)}
                        </span>
                        <span className="pill pill-muted">{item.reason}</span>
                      </div>
                      <div style={{ fontSize: 12.5, color: '#e9d5ff' }}>“{item.subtitleText}”</div>
                      <div style={{ display: 'flex', gap: 8 }}>
                        {item.approved ? (
                          <span className="pill pill-emerald font-mono">
                            <CheckCircle2 size={13} /> APPROVED • IN QUEUE
                          </span>
                        ) : (
                          <>
                            <button
                              type="button"
                              className="btn btn-emerald btn-sm"
                              style={{ flex: 1 }}
                              onClick={() => void handleApproveSuggestion(item)}
                            >
                              <CheckCircle2 size={14} />
                              Approve & Add
                            </button>
                            <button
                              type="button"
                              className="btn btn-ghost btn-sm"
                              onClick={() => handleDismissSuggestion(item.id)}
                            >
                              Dismiss
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>

            {/* Staged Clips Queue + Real Browser MP4 Export */}
            {activeSession.stagedClips.length > 0 && (
              <section className="card stack">
                <div className="row-between">
                  <h2 style={{ fontSize: 17 }}>
                    Ready Clips Queue ({activeSession.stagedClips.length})
                  </h2>
                  <button
                    type="button"
                    className="btn btn-emerald btn-sm"
                    disabled={exportingClipId !== null}
                    onClick={() =>
                      void onExportAllMp4(
                        activeSession.stagedClips,
                        activeSession.sourceFile,
                        activeSession.metadata.fileName
                      )
                    }
                  >
                    <Download size={14} />
                    Export All MP4
                  </button>
                </div>

                <div className="stack" style={{ gap: 10 }}>
                  {activeSession.stagedClips.map((clip) => {
                    const isExporting = exportingClipId === clip.id;
                    const isPreview = previewingClipId === clip.id;
                    return (
                      <article
                        key={clip.id}
                        className="card stack"
                        style={{
                          padding: 12,
                          gap: 10,
                          borderColor: isPreview ? 'var(--border-bright)' : 'var(--border-subtle)',
                        }}
                      >
                        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
                          {clip.thumbnailDataUrl ? (
                            <img
                              src={clip.thumbnailDataUrl}
                              alt={clip.title}
                              style={{
                                width: 84,
                                height: 60,
                                objectFit: 'cover',
                                borderRadius: 10,
                                flexShrink: 0,
                              }}
                            />
                          ) : (
                            <div
                              style={{
                                width: 84,
                                height: 60,
                                borderRadius: 10,
                                background: '#0c0916',
                                display: 'grid',
                                placeItems: 'center',
                              }}
                            >
                              <Film size={20} />
                            </div>
                          )}
                          <div style={{ flex: 1, minWidth: 0 }}>
                            <div style={{ fontWeight: 700, fontSize: 14.5 }}>{clip.title}</div>
                            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginTop: 4 }}>
                              <span className="pill font-mono">
                                {formatTimecode(clip.startSec)} – {formatTimecode(clip.endSec)}
                              </span>
                              <span className="pill pill-muted font-mono">{clip.aspectRatio}</span>
                            </div>
                            {clip.subtitleText && (
                              <div
                                style={{
                                  fontSize: 12,
                                  color: '#d8b4fe',
                                  marginTop: 4,
                                  overflow: 'hidden',
                                  textOverflow: 'ellipsis',
                                  whiteSpace: 'nowrap',
                                }}
                              >
                                Subtitles: “{clip.subtitleText}”
                              </div>
                            )}
                          </div>
                        </div>

                        {isExporting && (
                          <div className="stack" style={{ gap: 5 }}>
                            <div className="progress-track">
                              <div className="progress-fill" style={{ width: `${exportProgress}%` }} />
                            </div>
                            <div style={{ fontSize: 12, color: 'var(--emerald-ready)' }}>
                              {exportStage || `Exporting MP4… ${exportProgress}%`}
                            </div>
                          </div>
                        )}

                        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => setPreviewingClipId(isPreview ? null : clip.id)}
                          >
                            <Play size={14} />
                            {isPreview ? 'Playing' : 'Preview'}
                          </button>
                          <button
                            type="button"
                            className="btn btn-ghost btn-sm"
                            onClick={() => openEditClipModal(clip)}
                          >
                            <Edit3 size={14} />
                            Edit
                          </button>
                          <button
                            type="button"
                            className="btn btn-emerald btn-sm"
                            disabled={exportingClipId !== null}
                            onClick={() =>
                              void onExportClipMp4(
                                clip,
                                activeSession.sourceFile,
                                activeSession.metadata.fileName
                              )
                            }
                          >
                            <Download size={14} />
                            Download MP4
                          </button>
                          <button
                            type="button"
                            className="btn btn-ghost btn-sm"
                            onClick={() => handleDownloadSrt(clip)}
                          >
                            <FileText size={14} />
                            SRT
                          </button>
                          <button
                            type="button"
                            className="btn btn-danger btn-sm"
                            aria-label={`Remove ${clip.title}`}
                            onClick={() => handleRemoveStagedClip(clip.id)}
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>
                      </article>
                    );
                  })}
                </div>
              </section>
            )}

            {/* Save Project & Clips to My Projects */}
            <button
              type="button"
              className="btn btn-primary"
              disabled={isSaving || exportingClipId !== null}
              onClick={() => void handleSaveWorkspace()}
            >
              <Save size={17} />
              {isSaving
                ? 'Saving to My Projects…'
                : activeSession.stagedClips.length > 0
                  ? `Save Project & ${activeSession.stagedClips.length} Clip(s) to My Projects`
                  : 'Save Project + Current Trimmed Clip'}
            </button>
          </div>
        </div>
      )}

      {/* Inline Staged Clip Edit Modal */}
      {editingClipId && activeSession && (
        <div className="modal-backdrop" onClick={() => setEditingClipId(null)}>
          <div className="modal-card stack" onClick={(e) => e.stopPropagation()}>
            <div className="row-between">
              <h3 style={{ fontSize: 18 }}>Edit Clip & Subtitles</h3>
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                onClick={() => setEditingClipId(null)}
              >
                <X size={16} />
              </button>
            </div>

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
              />
            </div>

            <div className="row-between">
              <span className="pill font-mono">IN {formatTimecode(editStartSec)}</span>
              <span className="pill pill-amber font-mono">
                LEN {formatTimecode(Math.max(0, editEndSec - editStartSec))}
              </span>
              <span className="pill font-mono">OUT {formatTimecode(editEndSec)}</span>
            </div>

            <div>
              <label className="field-label">Start Time</label>
              <input
                type="range"
                min={0}
                max={Math.max(1, activeSession.metadata.durationSec - 1)}
                step={0.5}
                value={editStartSec}
                onChange={(e) => {
                  const next = Number.parseFloat(e.target.value);
                  setEditStartSec(next);
                  if (next >= editEndSec - 1) {
                    setEditEndSec(Math.min(activeSession.metadata.durationSec, next + 1));
                  }
                }}
                className="timeline-range"
              />
            </div>

            <div>
              <label className="field-label">End Time</label>
              <input
                type="range"
                min={Math.min(activeSession.metadata.durationSec, editStartSec + 1)}
                max={Math.max(1, activeSession.metadata.durationSec)}
                step={0.5}
                value={editEndSec}
                onChange={(e) => setEditEndSec(Number.parseFloat(e.target.value))}
                className="timeline-range"
              />
            </div>

            <div style={{ display: 'flex', gap: 10 }}>
              <button
                type="button"
                className="btn btn-primary"
                style={{ flex: 1 }}
                onClick={saveEditClipModal}
              >
                Save Changes
              </button>
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => setEditingClipId(null)}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
