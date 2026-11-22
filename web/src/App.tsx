import React, { useCallback, useEffect, useState } from 'react';
import {
  Clapperboard,
  Film,
  FolderOpen,
  Home,
  Scissors,
  Settings as SettingsIcon,
  ShieldCheck,
} from 'lucide-react';
import { ClipsPage } from './pages/ClipsPage';
import { HomePage } from './pages/HomePage';
import { ProjectsPage } from './pages/ProjectsPage';
import { SettingsPage } from './pages/SettingsPage';
import { ActiveEditorSession, ShortsEditorPage } from './pages/ShortsEditorPage';
import {
  clearAllLocalStorage,
  deleteProjectVideoBlob,
  getProjectVideoBlob,
  loadClips,
  loadExports,
  loadProjects,
  loadSettings,
  saveClips,
  saveExports,
  saveProjects,
  saveSettings,
  storeProjectVideoBlob,
} from './services/storage';
import { trimAndExportMp4, triggerBrowserDownload } from './services/videoProcessor';
import {
  AspectRatioOption,
  CaptionStyleOption,
  ClipItem,
  DEFAULT_STUDIO_SETTINGS,
  ExportHistoryItem,
  StudioSettings,
  StudioTab,
  VideoProject,
} from './types/studio';

interface NavEntry {
  id: StudioTab;
  label: string;
  icon: React.ReactNode;
}

const NAV_ITEMS: NavEntry[] = [
  { id: 'home', label: 'Home', icon: <Home size={19} /> },
  { id: 'editor', label: 'Shorts Editor', icon: <Scissors size={19} /> },
  { id: 'projects', label: 'My Projects', icon: <FolderOpen size={19} /> },
  { id: 'clips', label: 'My Clips', icon: <Film size={19} /> },
  { id: 'settings', label: 'Settings', icon: <SettingsIcon size={19} /> },
];

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<StudioTab>('home');
  const [projects, setProjects] = useState<VideoProject[]>(() => loadProjects());
  const [clips, setClips] = useState<ClipItem[]>(() => loadClips());
  const [exportsHistory, setExportsHistory] = useState<ExportHistoryItem[]>(() => loadExports());
  const [settings, setSettings] = useState<StudioSettings>(() => loadSettings());

  const [editorSession, setEditorSession] = useState<ActiveEditorSession | null>(null);
  const [exportingClipId, setExportingClipId] = useState<string | null>(null);
  const [exportProgress, setExportProgress] = useState(0);
  const [exportStage, setExportStage] = useState('');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = useCallback((msg: string) => {
    setToastMessage(msg);
  }, []);

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 3800);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  useEffect(() => saveProjects(projects), [projects]);
  useEffect(() => saveClips(clips), [clips]);
  useEffect(() => saveExports(exportsHistory), [exportsHistory]);
  useEffect(() => saveSettings(settings), [settings]);

  const handleSaveProjectAndClips = async (
    session: ActiveEditorSession,
    aspectRatio: AspectRatioOption,
    captionStyle: CaptionStyleOption
  ): Promise<VideoProject> => {
    const now = Date.now();
    const projectId = session.projectId ?? `proj_${now}`;
    const cleanTitle = session.projectTitle.trim() || session.metadata.fileName;

    const projectRecord: VideoProject = {
      id: projectId,
      title: cleanTitle,
      fileName: session.metadata.fileName,
      formatLabel: session.metadata.formatLabel,
      durationSec: session.metadata.durationSec,
      width: session.metadata.width,
      height: session.metadata.height,
      fileSizeBytes: session.metadata.fileSizeBytes,
      thumbnailDataUrl: session.metadata.thumbnailDataUrl,
      aspectRatio,
      captionStyle,
      createdAt: projects.find((p) => p.id === projectId)?.createdAt ?? now,
      updatedAt: now,
    };

    const persistedBlob = await storeProjectVideoBlob(projectId, session.sourceFile);

    setProjects((prev) => [projectRecord, ...prev.filter((p) => p.id !== projectId)]);

    const normalizedClips = session.stagedClips.map((c) => ({
      ...c,
      projectId,
      projectTitle: cleanTitle,
    }));

    setClips((prev) => [...normalizedClips, ...prev.filter((c) => c.projectId !== projectId)]);

    setEditorSession({
      ...session,
      projectId,
      projectTitle: cleanTitle,
      stagedClips: normalizedClips,
    });

    showToast(
      persistedBlob
        ? `Saved "${cleanTitle}" with ${normalizedClips.length} clip(s) to My Projects`
        : `Saved "${cleanTitle}" metadata (${normalizedClips.length} clips). Video was too large for browser cache.`
    );
    return projectRecord;
  };

  const handleResumeProject = async (project: VideoProject) => {
    const blob = await getProjectVideoBlob(project.id);
    if (!blob) {
      showToast('Original local video is no longer cached in this browser. Re-select the video file.');
      setActiveTab('editor');
      return;
    }
    const projectClips = clips.filter((c) => c.projectId === project.id);
    setEditorSession({
      projectId: project.id,
      projectTitle: project.title,
      sourceFile: blob,
      metadata: {
        fileName: project.fileName,
        formatLabel: project.formatLabel,
        durationSec: project.durationSec,
        width: project.width,
        height: project.height,
        fileSizeBytes: project.fileSizeBytes,
        thumbnailDataUrl: project.thumbnailDataUrl,
      },
      stagedClips: projectClips,
    });
    setActiveTab('editor');
    showToast(`Resumed "${project.title}"`);
  };

  const handleDeleteProject = async (projectId: string) => {
    setProjects((prev) => prev.filter((p) => p.id !== projectId));
    setClips((prev) => prev.filter((c) => c.projectId !== projectId));
    if (editorSession?.projectId === projectId) {
      setEditorSession(null);
    }
    await deleteProjectVideoBlob(projectId);
    showToast('Project deleted from local workspace');
  };

  const runMp4Export = async (clip: ClipItem, sourceFile: Blob, sourceFileName: string) => {
    if (exportingClipId) return;
    setExportingClipId(clip.id);
    setExportProgress(5);
    setExportStage('Preparing browser FFmpeg.wasm…');
    try {
      const targetAspect: AspectRatioOption =
        settings.autoConvert9x16 && clip.aspectRatio !== '9:16' ? '9:16' : clip.aspectRatio;

      const result = await trimAndExportMp4({
        sourceFile,
        sourceFileName,
        clipTitle: clip.title,
        startSec: clip.startSec,
        endSec: clip.endSec,
        aspectRatio: targetAspect,
        smartCropMode: clip.smartCropMode,
        quality: settings.exportQuality,
        onProgress: (pct, stage) => {
          setExportProgress(pct);
          setExportStage(stage);
        },
      });

      triggerBrowserDownload(result.blob, result.fileName);

      const historyEntry: ExportHistoryItem = {
        id: `exp_${Date.now()}`,
        fileName: result.fileName,
        clipTitle: clip.title,
        aspectRatio: targetAspect,
        quality: settings.exportQuality,
        sizeBytes: result.sizeBytes,
        durationSec: result.durationSec,
        createdAt: Date.now(),
      };
      setExportsHistory((prev) => [historyEntry, ...prev].slice(0, 50));
      showToast(`Downloaded ${result.fileName}`);
    } catch (err) {
      showToast(
        err instanceof Error
          ? err.message
          : 'Browser MP4 export failed. Try 720p quality or a shorter clip.'
      );
    } finally {
      setExportingClipId(null);
      setExportProgress(0);
      setExportStage('');
    }
  };

  const handleExportAllMp4 = async (
    clipList: ClipItem[],
    sourceFile: Blob,
    sourceFileName: string
  ) => {
    for (let i = 0; i < clipList.length; i++) {
      showToast(`Exporting clip ${i + 1} of ${clipList.length}…`);
      await runMp4Export(clipList[i], sourceFile, sourceFileName);
    }
  };

  const handleExportSavedClip = async (clip: ClipItem) => {
    const project = projects.find((p) => p.id === clip.projectId);
    const blob = await getProjectVideoBlob(clip.projectId);
    if (!blob) {
      showToast('Original local video is not in browser cache. Resume project & re-select file.');
      return;
    }
    await runMp4Export(clip, blob, project?.fileName ?? 'video.mp4');
  };

  const handleToggleFavorite = (clipId: string) => {
    setClips((prev) =>
      prev.map((c) => (c.id === clipId ? { ...c, isFavorite: !c.isFavorite } : c))
    );
  };

  const handleUpdateClip = (
    clipId: string,
    updates: Partial<
      Pick<ClipItem, 'title' | 'subtitleText' | 'startSec' | 'endSec' | 'aspectRatio' | 'captionStyle'>
    >
  ) => {
    setClips((prev) => prev.map((c) => (c.id === clipId ? { ...c, ...updates } : c)));
    showToast('Clip updated');
  };

  const handleDeleteClip = (clipId: string) => {
    setClips((prev) => prev.filter((c) => c.id !== clipId));
    showToast('Clip deleted');
  };

  const handleClearAllStorage = async () => {
    await clearAllLocalStorage();
    setProjects([]);
    setClips([]);
    setExportsHistory([]);
    setEditorSession(null);
    showToast('Cleared all local browser projects and video cache');
  };

  return (
    <div className="app-shell">
      {/* Desktop Sidebar Navigation */}
      <aside className="desktop-sidebar" aria-label="Studio Navigation">
        <div className="brand-row">
          <div className="brand-mark">
            <Clapperboard size={22} />
          </div>
          <div>
            <div className="brand-name">
              YodhaClip <span className="brand-ai-pill">AI</span>
            </div>
            <div style={{ fontSize: 11.5, color: 'var(--text-secondary)' }}>Shorts Web Studio</div>
          </div>
        </div>

        <nav className="sidebar-nav-list">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`sidebar-nav-item ${activeTab === item.id ? 'active' : ''}`}
              onClick={() => setActiveTab(item.id)}
            >
              {item.icon}
              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        <div style={{ marginTop: 'auto' }} className="card">
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
            <ShieldCheck size={16} color="#10b981" />
            <span style={{ fontSize: 12.5, fontWeight: 700 }}>Free Local Engine</span>
          </div>
          <p style={{ margin: 0, fontSize: 11.5, color: 'var(--text-secondary)', lineHeight: 1.45 }}>
            Videos stay privately on your device and export via browser FFmpeg.wasm.
          </p>
        </div>
      </aside>

      {/* Main Responsive Workspace */}
      <main className="main-workspace">
        {activeTab === 'home' && (
          <HomePage
            projects={projects}
            clips={clips}
            exportsHistory={exportsHistory}
            onNavigate={setActiveTab}
            onResumeProject={(proj) => void handleResumeProject(proj)}
          />
        )}

        {activeTab === 'editor' && (
          <ShortsEditorPage
            settings={settings}
            activeSession={editorSession}
            exportingClipId={exportingClipId}
            exportProgress={exportProgress}
            exportStage={exportStage}
            onSessionChange={setEditorSession}
            onSaveProjectAndClips={handleSaveProjectAndClips}
            onExportClipMp4={runMp4Export}
            onExportAllMp4={handleExportAllMp4}
            onNotify={showToast}
          />
        )}

        {activeTab === 'projects' && (
          <ProjectsPage
            projects={projects}
            clips={clips}
            onCreateNew={() => {
              setEditorSession(null);
              setActiveTab('editor');
            }}
            onResumeProject={(proj) => void handleResumeProject(proj)}
            onDeleteProject={(id) => void handleDeleteProject(id)}
          />
        )}

        {activeTab === 'clips' && (
          <ClipsPage
            clips={clips}
            settings={settings}
            exportingClipId={exportingClipId}
            exportProgress={exportProgress}
            exportStage={exportStage}
            onCreateNew={() => setActiveTab('editor')}
            onToggleFavorite={handleToggleFavorite}
            onUpdateClip={handleUpdateClip}
            onDeleteClip={handleDeleteClip}
            onExportClipMp4={(clip) => void handleExportSavedClip(clip)}
          />
        )}

        {activeTab === 'settings' && (
          <SettingsPage
            settings={settings}
            exportsHistory={exportsHistory}
            projectCount={projects.length}
            clipCount={clips.length}
            onUpdateSettings={(patch) => setSettings((prev) => ({ ...prev, ...patch }))}
            onResetSettings={() => {
              setSettings(DEFAULT_STUDIO_SETTINGS);
              showToast('Settings reset to defaults');
            }}
            onClearAllStorage={() => void handleClearAllStorage()}
          />
        )}
      </main>

      {/* Mobile Bottom Navigation */}
      <nav className="mobile-bottom-nav" aria-label="Mobile Navigation">
        {NAV_ITEMS.map((item) => (
          <button
            key={item.id}
            type="button"
            className={`nav-item ${activeTab === item.id ? 'active' : ''}`}
            onClick={() => setActiveTab(item.id)}
          >
            {item.icon}
            <span>{item.label === 'Shorts Editor' ? 'Editor' : item.label.replace('My ', '')}</span>
          </button>
        ))}
      </nav>

      {toastMessage && (
        <div className="toast" role="status" aria-live="polite">
          {toastMessage}
        </div>
      )}
    </div>
  );
};
