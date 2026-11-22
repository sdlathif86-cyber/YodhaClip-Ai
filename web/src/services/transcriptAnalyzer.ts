import { HighlightSuggestion } from '../types/studio';

interface ParsedCue {
  startSec: number;
  text: string;
}

const HOOK_KEYWORDS = [
  'secret',
  'wait',
  'mistake',
  'stop',
  'never',
  'best',
  'top',
  'crazy',
  'insane',
  'clutch',
  'tip',
  'trick',
  'why',
  'how',
  'important',
  'watch',
  'reveal',
  'final',
  'win',
  'truth',
  'free',
];

function parseTimeToSec(token: string): number | null {
  const clean = token.replace(',', '.').trim();
  const parts = clean.split(':').map((p) => Number.parseFloat(p));
  if (parts.some((n) => Number.isNaN(n))) return null;
  if (parts.length === 2) return parts[0] * 60 + parts[1];
  if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2];
  return null;
}

function parseTranscriptCues(rawTranscript: string, totalDurationSec: number): ParsedCue[] {
  const lines = rawTranscript.split(/\r?\n/);
  const cues: ParsedCue[] = [];

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    if (!line || /^\d+$/.test(line)) continue;

    // SRT / VTT range line: 00:00:12,000 --> 00:00:16,000
    const srtMatch = line.match(/^(\d{1,2}:\d{2}(?::\d{2})?(?:[.,]\d+)?)\s*-->\s*(\d{1,2}:\d{2}(?::\d{2})?(?:[.,]\d+)?)/);
    if (srtMatch) {
      const start = parseTimeToSec(srtMatch[1]);
      const textLines: string[] = [];
      while (i + 1 < lines.length && lines[i + 1].trim() !== '' && !lines[i + 1].includes('-->')) {
        i++;
        textLines.push(lines[i].trim());
      }
      if (start !== null && textLines.length > 0) {
        cues.push({ startSec: start, text: textLines.join(' ') });
      }
      continue;
    }

    // Inline timestamp line: [01:25] text or 01:25 - text
    const inlineMatch = line.match(/^\[?(\d{1,2}:\d{2}(?::\d{2})?)\]?\s*[-–:]?\s*(.+)$/);
    if (inlineMatch) {
      const start = parseTimeToSec(inlineMatch[1]);
      if (start !== null) {
        cues.push({ startSec: start, text: inlineMatch[2].trim() });
      }
      continue;
    }
  }

  // Fallback for plain untimestamped transcript: estimate timestamps evenly across sentence groups
  if (cues.length === 0) {
    const sentences = rawTranscript
      .replace(/\s+/g, ' ')
      .split(/(?<=[.!?])\s+/)
      .map((s) => s.trim())
      .filter((s) => s.length >= 8);

    if (sentences.length > 0) {
      const step = Math.max(4, totalDurationSec / sentences.length);
      sentences.forEach((sentence, idx) => {
        cues.push({
          startSec: Math.min(totalDurationSec - 5, Math.floor(idx * step)),
          text: sentence,
        });
      });
    }
  }

  return cues;
}

function scoreSentence(text: string): { score: number; reason: string } {
  const lower = text.toLowerCase();
  let score = 68;
  const reasons: string[] = [];

  const matchedKeywords = HOOK_KEYWORDS.filter((kw) => lower.includes(kw));
  if (matchedKeywords.length > 0) {
    score += Math.min(18, matchedKeywords.length * 6);
    reasons.push(`Strong hook cue ("${matchedKeywords[0]}")`);
  }
  if (text.includes('?')) {
    score += 7;
    reasons.push('Curiosity question');
  }
  if (text.includes('!')) {
    score += 6;
    reasons.push('High energy emphasis');
  }
  if (/\d+/.test(text)) {
    score += 4;
    reasons.push('Concrete stat/number');
  }

  return {
    score: Math.min(97, score),
    reason: reasons.length > 0 ? reasons.join(' • ') : 'Natural sentence opening boundary',
  };
}

/**
 * 100% free, local browser-side transcript highlight detector.
 * Never calls paid APIs and never sends private videos to external servers.
 */
export function analyzeTranscriptLocally(
  rawTranscript: string,
  videoDurationSec: number,
  targetDurationSec: number,
  maxSuggestions = 6
): HighlightSuggestion[] {
  const trimmed = rawTranscript.trim();
  if (trimmed.length < 15) {
    return [];
  }

  const safeDuration = Math.max(5, videoDurationSec);
  const clipLen = Math.min(safeDuration, Math.max(10, targetDurationSec));
  const cues = parseTranscriptCues(trimmed, safeDuration);
  if (cues.length === 0) return [];

  const scored = cues.map((cue, index) => {
    const { score, reason } = scoreSentence(cue.text);
    const startSec = Math.max(0, Math.min(Math.floor(cue.startSec), Math.max(0, safeDuration - 5)));
    const endSec = Math.min(safeDuration, startSec + clipLen);
    const cleanTitle =
      cue.text
        .replace(/[^\p{L}\p{N}\s?!'-]/gu, '')
        .split(/\s+/)
        .slice(0, 7)
        .join(' ')
        .trim() || `Highlight #${index + 1}`;

    return {
      id: `local_hl_${index}_${startSec}`,
      title: cleanTitle,
      startSec,
      endSec: Math.max(startSec + 3, endSec),
      score,
      reason,
      subtitleText: cue.text.slice(0, 140),
      approved: false,
    };
  });

  // Sort by score descending and de-duplicate heavily overlapping windows
  scored.sort((a, b) => b.score - a.score);
  const selected: HighlightSuggestion[] = [];
  for (const candidate of scored) {
    if (selected.length >= maxSuggestions) break;
    const overlaps = selected.some((existing) => Math.abs(existing.startSec - candidate.startSec) < clipLen * 0.5);
    if (!overlaps) {
      selected.push(candidate);
    }
  }

  return selected.sort((a, b) => a.startSec - b.startSec);
}
