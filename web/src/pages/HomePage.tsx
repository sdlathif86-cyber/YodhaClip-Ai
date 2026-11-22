import React from 'react';
import {
  Clapperboard,
  Clock,
  Download,
  Film,
  FolderOpen,
  Play,
  Scissors,
  Settings,
  ShieldCheck,
  Sparkles,
  Upload,
} from 'lucide-react';
import {
  ClipItem,
  ExportHistoryItem,
  StudioTab,
  VideoProject,
  formatTimecode,
} from '../types/studio';

interface HomePageProps {
  projects: VideoProject[];
  clips: ClipItem[];
  exportsHistory: ExportHistoryItem[];
  onNavigate: (tab: StudioTab) => void;
  onResumeProject: (project: VideoProject) => void;
}

export const HomePage: React.FC<HomePageProps> = ({
  projects,
  clips,
  exportsHistory,
  onNavigate,
  onResumeProject,
}) => {
  const recentProjects = projects.slice(0, 4);

  return (
    <div className="stack">
      {/* Mobile Top Brand Row */}
      <div className="row-between">
        <div className="brand-row">
          <div className="brand-mark">
            <Clapperboard size={22} />
          </div>
          <div>
            <div className="brand-name">
              YodhaClip <span className="brand-ai-pill">AI</span>
            </div>
            <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
              Long Videos Into Powerful Shorts.
            </div>
          </div>
        </div>

        <button
          type="button"
          className="btn btn-primary btn-sm"
          onClick={() => onNavigate('editor')}
        >
          <Upload size={15} />
          Upload Video
        </button>
      </div>

      {/* Hero Dashboard Card */}
      <section className="hero-card stack" style={{ gap: 14 }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          <span className="pill font-mono">
            <Sparkles size={13} /> BROWSER SHORTS STUDIO • FFMPEG.WASM
          </span>
          <span className="pill pill-emerald font-mono">
            <ShieldCheck size={13} /> 100% FREE LOCAL PROCESSING
          </span>
        </div>

        <h1 style={{ fontSize: 'clamp(24px, 4vw, 34px)', lineHeight: 1.15 }}>
          Long Videos Into Powerful Shorts.
        </h1>
        <p style={{ margin: 0, color: 'var(--text-secondary)', maxWidth: 640, lineHeight: 1.55 }}>
          Upload MP4, MOV, or WebM videos right in your mobile or desktop browser. Trim multiple
          9:16 vertical Shorts, add editable subtitles, analyze transcripts locally, and download
          real MP4 files with zero paid APIs or cloud uploads.
        </p>

        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => onNavigate('editor')}
          >
            <Scissors size={18} />
            Create New Shorts
          </button>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => onNavigate('projects')}
          >
            <FolderOpen size={17} />
            My Projects ({projects.length})
          </button>
        </div>
      </section>

      {/* Studio Metrics */}
      <section className="stats-grid">
        <div className="card">
          <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>Projects</div>
          <div className="font-display" style={{ fontSize: 28, fontWeight: 700, marginTop: 4 }}>
            {projects.length}
          </div>
        </div>
        <div className="card">
          <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>Total Clips Created</div>
          <div
            className="font-display"
            style={{ fontSize: 28, fontWeight: 700, marginTop: 4, color: 'var(--violet-bright)' }}
          >
            {clips.length}
          </div>
        </div>
        <div className="card">
          <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>MP4 Exports</div>
          <div
            className="font-display"
            style={{ fontSize: 28, fontWeight: 700, marginTop: 4, color: 'var(--emerald-ready)' }}
          >
            {exportsHistory.length}
          </div>
        </div>
      </section>

      {/* Quick Studio Actions */}
      <section className="stack" style={{ gap: 10 }}>
        <h2 style={{ fontSize: 18 }}>Quick Studio Actions</h2>
        <div className="quick-grid">
          <button
            type="button"
            className="card"
            style={{ textAlign: 'left', cursor: 'pointer' }}
            onClick={() => onNavigate('editor')}
          >
            <Upload size={22} color="#a855f7" />
            <div style={{ fontWeight: 700, marginTop: 10 }}>Shorts Editor</div>
            <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 3 }}>
              Upload MP4/MOV/WebM & trim 9:16 clips
            </div>
          </button>

          <button
            type="button"
            className="card"
            style={{ textAlign: 'left', cursor: 'pointer' }}
            onClick={() => onNavigate('projects')}
          >
            <FolderOpen size={22} color="#d946ef" />
            <div style={{ fontWeight: 700, marginTop: 10 }}>My Projects</div>
            <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 3 }}>
              Resume saved videos & timelines
            </div>
          </button>

          <button
            type="button"
            className="card"
            style={{ textAlign: 'left', cursor: 'pointer' }}
            onClick={() => onNavigate('clips')}
          >
            <Film size={22} color="#10b981" />
            <div style={{ fontWeight: 700, marginTop: 10 }}>My Clips</div>
            <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 3 }}>
              Preview, edit subtitles & export MP4
            </div>
          </button>

          <button
            type="button"
            className="card"
            style={{ textAlign: 'left', cursor: 'pointer' }}
            onClick={() => onNavigate('settings')}
          >
            <Settings size={22} color="#f59e0b" />
            <div style={{ fontWeight: 700, marginTop: 10 }}>Settings</div>
            <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 3 }}>
              720p/1080p quality, language & storage
            </div>
          </button>
        </div>
      </section>

      {/* Recent Projects */}
      <section className="stack" style={{ gap: 12 }}>
        <div className="row-between">
          <h2 style={{ fontSize: 18 }}>Recent Projects</h2>
          {projects.length > 0 && (
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              onClick={() => onNavigate('projects')}
            >
              View All
            </button>
          )}
        </div>

        {recentProjects.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: 28 }}>
            <Download size={28} color="#a855f7" style={{ marginBottom: 8 }} />
            <div style={{ fontWeight: 700, fontSize: 16 }}>No video projects yet</div>
            <p style={{ color: 'var(--text-secondary)', fontSize: 13.5, margin: '6px 0 16px' }}>
              Import a local MP4, MOV, or WebM video to start creating 9:16 Shorts in your browser.
            </p>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => onNavigate('editor')}
            >
              <Upload size={16} />
              Start First Project
            </button>
          </div>
        ) : (
          <div className="projects-grid">
            {recentProjects.map((project) => {
              const projectClips = clips.filter((c) => c.projectId === project.id);
              const totalClipDuration = projectClips.reduce(
                (acc, c) => acc + Math.max(0, c.endSec - c.startSec),
                0
              );
              return (
                <article key={project.id} className="card stack" style={{ gap: 10 }}>
                  {project.thumbnailDataUrl ? (
                    <img src={project.thumbnailDataUrl} alt={project.title} className="thumb-img" />
                  ) : (
                    <div
                      className="thumb-img"
                      style={{ display: 'grid', placeItems: 'center', color: 'var(--text-muted)' }}
                    >
                      <Film size={28} />
                    </div>
                  )}
                  <div style={{ fontWeight: 700, fontSize: 15 }}>{project.title}</div>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                    <span className="pill font-mono">{project.formatLabel}</span>
                    <span className="pill pill-muted font-mono">
                      <Clock size={11} /> {formatTimecode(project.durationSec)}
                    </span>
                    <span className="pill pill-emerald font-mono">
                      {projectClips.length} clips ({formatTimecode(totalClipDuration)})
                    </span>
                  </div>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => onResumeProject(project)}
                  >
                    <Play size={14} />
                    Resume Editing
                  </button>
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
};
