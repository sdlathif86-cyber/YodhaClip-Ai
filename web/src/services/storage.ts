import {
  ClipItem,
  DEFAULT_STUDIO_SETTINGS,
  ExportHistoryItem,
  StudioSettings,
  VideoProject,
} from '../types/studio';

const PROJECTS_KEY = 'yodhaclip_web_projects_v1';
const CLIPS_KEY = 'yodhaclip_web_clips_v1';
const EXPORTS_KEY = 'yodhaclip_web_exports_v1';
const SETTINGS_KEY = 'yodhaclip_web_settings_v1';

const DB_NAME = 'yodhaclip_media_db';
const DB_VERSION = 1;
const VIDEO_STORE = 'project_videos';

function readJson<T>(key: string, fallback: T): T {
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

function writeJson<T>(key: string, value: T): void {
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Storage quota exceeded — metadata stays in memory for this session
  }
}

export function loadProjects(): VideoProject[] {
  return readJson<VideoProject[]>(PROJECTS_KEY, []);
}

export function saveProjects(projects: VideoProject[]): void {
  writeJson(PROJECTS_KEY, projects);
}

export function loadClips(): ClipItem[] {
  return readJson<ClipItem[]>(CLIPS_KEY, []);
}

export function saveClips(clips: ClipItem[]): void {
  writeJson(CLIPS_KEY, clips);
}

export function loadExports(): ExportHistoryItem[] {
  return readJson<ExportHistoryItem[]>(EXPORTS_KEY, []);
}

export function saveExports(exportsList: ExportHistoryItem[]): void {
  writeJson(EXPORTS_KEY, exportsList);
}

export function loadSettings(): StudioSettings {
  return {
    ...DEFAULT_STUDIO_SETTINGS,
    ...readJson<Partial<StudioSettings>>(SETTINGS_KEY, {}),
  };
}

export function saveSettings(settings: StudioSettings): void {
  writeJson(SETTINGS_KEY, settings);
}

function openMediaDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (typeof indexedDB === 'undefined') {
      reject(new Error('IndexedDB unavailable'));
      return;
    }
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(VIDEO_STORE)) {
        db.createObjectStore(VIDEO_STORE);
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error ?? new Error('Failed to open local media DB'));
  });
}

export async function storeProjectVideoBlob(projectId: string, file: Blob): Promise<boolean> {
  try {
    const db = await openMediaDb();
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(VIDEO_STORE, 'readwrite');
      tx.objectStore(VIDEO_STORE).put(file, projectId);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error ?? new Error('Failed to persist local video'));
    });
    db.close();
    return true;
  } catch {
    return false;
  }
}

export async function getProjectVideoBlob(projectId: string): Promise<Blob | null> {
  try {
    const db = await openMediaDb();
    const blob = await new Promise<Blob | null>((resolve, reject) => {
      const tx = db.transaction(VIDEO_STORE, 'readonly');
      const req = tx.objectStore(VIDEO_STORE).get(projectId);
      req.onsuccess = () => resolve((req.result as Blob | undefined) ?? null);
      req.onerror = () => reject(req.error ?? new Error('Failed to load local video'));
    });
    db.close();
    return blob;
  } catch {
    return null;
  }
}

export async function deleteProjectVideoBlob(projectId: string): Promise<void> {
  try {
    const db = await openMediaDb();
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(VIDEO_STORE, 'readwrite');
      tx.objectStore(VIDEO_STORE).delete(projectId);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error ?? new Error('Failed to delete local video'));
    });
    db.close();
  } catch {
    // Ignore cleanup errors
  }
}

export async function clearAllLocalStorage(): Promise<void> {
  try {
    window.localStorage.removeItem(PROJECTS_KEY);
    window.localStorage.removeItem(CLIPS_KEY);
    window.localStorage.removeItem(EXPORTS_KEY);
    const db = await openMediaDb();
    await new Promise<void>((resolve) => {
      const tx = db.transaction(VIDEO_STORE, 'readwrite');
      tx.objectStore(VIDEO_STORE).clear();
      tx.oncomplete = () => resolve();
      tx.onerror = () => resolve();
    });
    db.close();
  } catch {
    // Ignore storage cleanup errors
  }
}
