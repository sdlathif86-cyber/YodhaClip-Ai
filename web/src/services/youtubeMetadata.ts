import { YouTubeOEmbedInfo } from '../types/studio';

export function extractYouTubeVideoId(rawUrl: string): string | null {
  const trimmed = rawUrl.trim();
  if (!trimmed) return null;

  if (/^[A-Za-z0-9_-]{11}$/.test(trimmed)) {
    return trimmed;
  }

  try {
    const parsed = new URL(trimmed.startsWith('http') ? trimmed : `https://${trimmed}`);
    const host = parsed.hostname.replace(/^www\./, '').toLowerCase();

    if (host === 'youtu.be') {
      const id = parsed.pathname.split('/').filter(Boolean)[0];
      return id && /^[A-Za-z0-9_-]{11}$/.test(id) ? id : null;
    }

    if (host === 'youtube.com' || host === 'm.youtube.com' || host === 'youtube-nocookie.com') {
      const watchId = parsed.searchParams.get('v');
      if (watchId && /^[A-Za-z0-9_-]{11}$/.test(watchId)) return watchId;

      const segments = parsed.pathname.split('/').filter(Boolean);
      if (segments.length >= 2 && ['shorts', 'embed', 'live'].includes(segments[0])) {
        const candidate = segments[1];
        return /^[A-Za-z0-9_-]{11}$/.test(candidate) ? candidate : null;
      }
    }
  } catch {
    return null;
  }

  return null;
}

export async function fetchYouTubeOEmbed(rawUrl: string): Promise<YouTubeOEmbedInfo> {
  const videoId = extractYouTubeVideoId(rawUrl);
  if (!videoId) {
    throw new Error('Enter a valid YouTube link (youtube.com/watch?v=... or youtu.be/...).');
  }

  const canonicalUrl = `https://www.youtube.com/watch?v=${videoId}`;
  const oembedUrl = `https://www.youtube.com/oembed?url=${encodeURIComponent(canonicalUrl)}&format=json`;
  const response = await fetch(oembedUrl);
  if (!response.ok) {
    throw new Error('Could not fetch public YouTube details. Verify the video is public and try again.');
  }

  const json = (await response.json()) as {
    title?: string;
    author_name?: string;
    thumbnail_url?: string;
  };

  return {
    videoId,
    title: json.title || 'YouTube Video',
    authorName: json.author_name || 'Creator',
    thumbnailUrl: json.thumbnail_url || `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
    sourceUrl: canonicalUrl,
  };
}
