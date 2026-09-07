import React, { useEffect, useRef, useState } from 'react';
import type { Lesson, ProgressSnapshot } from '../../../types/course';
import { usePlaybackTicket } from '../hooks/usePlaybackTicket';
import { useWatchTracker } from '../hooks/useWatchTracker';

/** Không tua tới sát đầu/cuối để tránh resume nhảy vào biên (FR-KT-01, ±5s). */
const RESUME_EDGE_GUARD_SECONDS = 5;

interface LessonVideoPlayerProps {
  courseId: number;
  lesson: Lesson;
  onProgress?: (snapshot: ProgressSnapshot) => void;
}

/**
 * Task 10 design_us15_us17.md — kết hợp usePlaybackTicket + useWatchTracker
 * thành một player hoàn chỉnh cho lesson VIDEO.
 */
export const LessonVideoPlayer: React.FC<LessonVideoPlayerProps> = ({ courseId, lesson, onProgress }) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hasResumedRef = useRef(false);
  const [renewAttempted, setRenewAttempted] = useState(false);

  const enabled = Boolean(lesson.playable);
  const { ticket, isLoading, error, isForbidden, renew } = usePlaybackTicket(courseId, lesson.id, enabled);

  useWatchTracker(courseId, lesson.id, videoRef, enabled && Boolean(ticket), onProgress);

  useEffect(() => {
    hasResumedRef.current = false;
    setRenewAttempted(false);
  }, [lesson.id]);

  // đổi src mà không mất vị trí (BR-26) — §8.3
  const swapSource = (nextUrl: string) => {
    const v = videoRef.current;
    if (!v) return;
    const { currentTime, paused, playbackRate } = v;
    v.src = nextUrl;
    const restore = () => {
      v.currentTime = currentTime;
      v.playbackRate = playbackRate;
      if (!paused) void v.play();
    };
    v.addEventListener('loadedmetadata', restore, { once: true });
  };

  const handleVideoError = async () => {
    if (renewAttempted) return; // tránh vòng lặp renew vô hạn nếu lỗi không phải do ticket
    setRenewAttempted(true);
    const next = await renew();
    if (next) swapSource(next.streamUrl);
  };

  const handleLoadedMetadata = () => {
    if (hasResumedRef.current) return;
    hasResumedRef.current = true;
    const v = videoRef.current;
    const resumeAt = lesson.lastPositionSeconds;
    if (!v || !resumeAt || resumeAt <= RESUME_EDGE_GUARD_SECONDS) return;
    const duration = v.duration || Infinity;
    if (resumeAt < duration - RESUME_EDGE_GUARD_SECONDS) {
      v.currentTime = resumeAt;
    }
  };

  if (!enabled) {
    return (
      <div className="text-center p-8 text-slate-400 space-y-3">
        <span className="material-symbols-outlined text-[48px]">lock</span>
        <h3 className="text-sm font-bold text-white m-0 font-display">Nội dung chưa sẵn sàng để phát</h3>
      </div>
    );
  }

  if (isForbidden) {
    return (
      <div className="text-center p-8 text-rose-400 space-y-3">
        <span className="material-symbols-outlined text-[48px]">block</span>
        <h3 className="text-sm font-bold text-white m-0 font-display">Bạn không còn quyền xem nội dung này</h3>
        <p className="text-xs text-slate-400 max-w-md mx-auto m-0">
          Vui lòng kiểm tra lại trạng thái ghi danh của bạn cho khóa học này.
        </p>
      </div>
    );
  }

  if (error && !ticket) {
    return (
      <div className="text-center p-8 text-rose-400 space-y-3">
        <span className="material-symbols-outlined text-[48px]">lock</span>
        <h3 className="text-sm font-bold text-white m-0 font-display">Không thể phát video</h3>
        <p className="text-xs text-slate-400 max-w-md mx-auto m-0">{error}</p>
      </div>
    );
  }

  if (!ticket) {
    return (
      <div className="text-center p-8 text-slate-400 space-y-3">
        <span
          className={`material-symbols-outlined text-[54px] text-primary-container ${
            isLoading ? 'animate-spin' : 'animate-bounce'
          }`}
        >
          {isLoading ? 'progress_activity' : 'play_circle'}
        </span>
        <h3 className="text-sm font-bold text-white m-0 font-display">
          {isLoading ? 'Đang tải video bài giảng...' : `Video bài giảng: ${lesson.title}`}
        </h3>
      </div>
    );
  }

  return (
    <video
      key={ticket.streamUrl}
      ref={videoRef}
      src={ticket.streamUrl}
      controls
      autoPlay
      onLoadedMetadata={handleLoadedMetadata}
      onError={handleVideoError}
      className="w-full h-full max-h-[60vh] object-contain"
    />
  );
};
