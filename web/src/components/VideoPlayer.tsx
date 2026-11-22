import React, { useEffect, useRef, useState } from 'react';
import { Pause, Play, RotateCcw, Volume2, VolumeX } from 'lucide-react';
import {
  AspectRatioOption,
  CaptionStyleOption,
  SmartCropMode,
  formatTimecode,
} from '../types/studio';

interface VideoPlayerProps {
  videoUrl: string;
  durationSec: number;
  aspectRatio: AspectRatioOption;
  captionStyle: CaptionStyleOption;
  smartCropMode?: SmartCropMode;
  subtitleText?: string;
  clipStartSec?: number;
  clipEndSec?: number;
  loop?: boolean;
  seekToSec?: number | null;
  onSeekConsumed?: () => void;
}

export const VideoPlayer: React.FC<VideoPlayerProps> = ({
  videoUrl,
  durationSec,
  aspectRatio,
  captionStyle,
  smartCropMode = 'CENTER',
  subtitleText = '',
  clipStartSec = 0,
  clipEndSec,
  loop = true,
  seekToSec = null,
  onSeekConsumed,
}) => {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [currentTime, setCurrentTime] = useState(clipStartSec);

  const effectiveEnd = Math.max(clipStartSec + 0.5, clipEndSec ?? durationSec);

  useEffect(() => {
    const el = videoRef.current;
    if (!el) return;
    if (seekToSec !== null && Number.isFinite(seekToSec)) {
      el.currentTime = Math.max(0, Math.min(seekToSec, durationSec));
      setCurrentTime(el.currentTime);
      onSeekConsumed?.();
    }
  }, [seekToSec, durationSec, onSeekConsumed]);

  useEffect(() => {
    const el = videoRef.current;
    if (!el) return;
    if (el.currentTime < clipStartSec || el.currentTime > effectiveEnd) {
      el.currentTime = clipStartSec;
      setCurrentTime(clipStartSec);
    }
  }, [clipStartSec, effectiveEnd]);

  const togglePlay = async () => {
    const el = videoRef.current;
    if (!el) return;
    if (el.paused) {
      if (el.currentTime < clipStartSec || el.currentTime >= effectiveEnd - 0.1) {
        el.currentTime = clipStartSec;
      }
      try {
        await el.play();
        setIsPlaying(true);
      } catch {
        setIsPlaying(false);
      }
    } else {
      el.pause();
      setIsPlaying(false);
    }
  };

  const toggleMute = () => {
    const el = videoRef.current;
    if (!el) return;
    el.muted = !el.muted;
    setIsMuted(el.muted);
  };

  const restartClip = () => {
    const el = videoRef.current;
    if (!el) return;
    el.currentTime = clipStartSec;
    setCurrentTime(clipStartSec);
  };

  const handleTimeUpdate = () => {
    const el = videoRef.current;
    if (!el) return;
    if (el.currentTime >= effectiveEnd) {
      if (loop) {
        el.currentTime = clipStartSec;
      } else {
        el.pause();
        setIsPlaying(false);
      }
    }
    setCurrentTime(el.currentTime);
  };

  const ratioClass =
    aspectRatio === '9:16'
      ? 'ratio-9-16'
      : aspectRatio === '1:1'
        ? 'ratio-1-1'
        : 'ratio-16-9';

  const objectPosition =
    smartCropMode === 'SPEAKER_TOP'
      ? '50% 15%'
      : smartCropMode === 'ACTION_LEFT'
        ? '15% 50%'
        : smartCropMode === 'ACTION_RIGHT'
          ? '85% 50%'
          : '50% 50%';

  const activeCaption =
    captionStyle === 'NONE'
      ? ''
      : subtitleText.trim() ||
        (captionStyle === 'BOLD_VIOLET'
          ? 'MAKE EVERY SECOND COUNT'
          : captionStyle === 'NEON_GLOW'
            ? 'VIRAL SHORT PREVIEW'
            : 'Clean studio subtitles');

  return (
    <div className="stack" style={{ gap: 10 }}>
      <div className="player-frame-outer">
        <div className={`player-stage ${ratioClass}`}>
          <video
            ref={videoRef}
            src={videoUrl}
            playsInline
            muted={isMuted}
            onTimeUpdate={handleTimeUpdate}
            onPause={() => setIsPlaying(false)}
            onPlay={() => setIsPlaying(true)}
            onClick={togglePlay}
            className="player-video"
            style={{ objectPosition }}
          />
          {activeCaption && (
            <div className="caption-overlay">
              <span className={`caption-box ${captionStyle}`}>{activeCaption}</span>
            </div>
          )}
        </div>
      </div>

      <div className="card" style={{ padding: '10px 12px', borderRadius: 14 }}>
        <div className="row-between" style={{ gap: 8 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <button
              type="button"
              className="btn btn-primary btn-sm"
              onClick={togglePlay}
              aria-label={isPlaying ? 'Pause video' : 'Play video'}
            >
              {isPlaying ? <Pause size={15} /> : <Play size={15} />}
              {isPlaying ? 'Pause' : 'Play'}
            </button>
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              onClick={restartClip}
              aria-label="Restart clip"
            >
              <RotateCcw size={15} />
            </button>
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              onClick={toggleMute}
              aria-label={isMuted ? 'Unmute video' : 'Mute video'}
            >
              {isMuted ? <VolumeX size={15} /> : <Volume2 size={15} />}
            </button>
          </div>
          <span className="pill pill-muted font-mono">
            {formatTimecode(currentTime)} / {formatTimecode(effectiveEnd)}
          </span>
        </div>

        <input
          type="range"
          min={clipStartSec}
          max={effectiveEnd}
          step={0.1}
          value={Math.min(Math.max(currentTime, clipStartSec), effectiveEnd)}
          onChange={(e) => {
            const next = Number.parseFloat(e.target.value);
            if (videoRef.current) {
              videoRef.current.currentTime = next;
            }
            setCurrentTime(next);
          }}
          aria-label="Seek video timeline"
          className="timeline-range"
          style={{ marginTop: 8 }}
        />
      </div>
    </div>
  );
};
