import React, { useMemo, useState } from 'react';
import { Clock, Film, Play, Search, Trash2, Upload } from 'lucide-react';
import { ClipItem, VideoProject, formatFileSize, formatTimecode } from '../types/studio';

interface ProjectsPageProps {
  projects: VideoProject[];
  clips: ClipItem[];
  onCreateNew: () => void;
  onResumeProject: (project: VideoProject) => void;
  onDeleteProject: (projectId: string) => void;
}

export const ProjectsPage: React.FC<ProjectsPageProps> = ({
  projects,
  clips,
  onCreateNew,
  onResumeProject,
  onDeleteProject,
}) => {
  const [query, setQuery] = useState('');

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return projects;
    return projects.filter(
      (p) => p.title.toLowerCase().includes(q) || p.fileName.toLowerCase().includes(q)
    );
  }, [projects, query]);

  return (
    <div className="stack">
      <div className="row-between">
        <div>
          <h1 style={{ fontSize: 26 }}>My Projects</h1>
          <p style={{ margin: '4px 0 0', fontSize: 13.5, color: 'var(--text-secondary)' }}>
            Saved local video timelines stored privately in your browser
          </p>
        </div>
        <button type="button" className="btn btn-primary btn-sm" onClick={onCreateNew}>
          <Upload size={15} />
          New Video
        </button>
      </div>

      {projects.length > 0 && (
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
            placeholder="Search projects by title or filename…"
            className="input"
            style={{ paddingLeft: 38 }}
          />
        </div>
      )}

      {filtered.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: 34 }}>
          <Film size={32} color="#a855f7" style={{ marginBottom: 10 }} />
          <div style={{ fontWeight: 700, fontSize: 17 }}>
            {projects.length === 0 ? 'Your workspace is empty' : 'No matching projects'}
          </div>
          <p style={{ color: 'var(--text-secondary)', fontSize: 13.5, margin: '6px 0 18px' }}>
            Upload an MP4, MOV, or WebM video in the Shorts Editor to save your first project.
          </p>
          <button type="button" className="btn btn-primary" onClick={onCreateNew}>
            <Upload size={16} />
            Open Shorts Editor
          </button>
        </div>
      ) : (
        <div className="projects-grid">
          {filtered.map((project) => {
            const projectClips = clips.filter((c) => c.projectId === project.id);
            const totalClipSec = projectClips.reduce(
              (sum, c) => sum + Math.max(0, c.endSec - c.startSec),
              0
            );
            return (
              <article key={project.id} className="card stack" style={{ gap: 12 }}>
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

                <div>
                  <h3 style={{ fontSize: 16 }}>{project.title}</h3>
                  <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 2 }}>
                    {project.fileName}
                  </div>
                </div>

                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                  <span className="pill font-mono">{project.formatLabel}</span>
                  <span className="pill pill-muted font-mono">
                    <Clock size={11} /> {formatTimecode(project.durationSec)}
                  </span>
                  <span className="pill pill-muted font-mono">
                    {project.width}×{project.height}
                  </span>
                  <span className="pill pill-muted font-mono">
                    {formatFileSize(project.fileSizeBytes)}
                  </span>
                  <span className="pill pill-emerald font-mono">
                    {projectClips.length} clips • {formatTimecode(totalClipSec)}
                  </span>
                </div>

                <div style={{ display: 'flex', gap: 8 }}>
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    style={{ flex: 1 }}
                    onClick={() => onResumeProject(project)}
                  >
                    <Play size={14} />
                    Resume Editing
                  </button>
                  <button
                    type="button"
                    className="btn btn-danger btn-sm"
                    aria-label={`Delete ${project.title}`}
                    onClick={() => onDeleteProject(project.id)}
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
};
