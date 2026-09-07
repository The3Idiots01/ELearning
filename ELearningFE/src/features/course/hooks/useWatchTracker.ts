import { useEffect, useRef, type RefObject } from 'react';
import { progressApi } from '../api/progressApi';
import type { ProgressSnapshot } from '../../../types/course';

const FLUSH_MS = 10_000;

/**
 * Đọc video.played + gửi heartbeat — §8.2 design_us15_us17.md. Không giữ
 * trạng thái tích luỹ nào phía client: video.played đã là trạng thái đó, và
 * trình duyệt tự loại phần tua qua (BR-27). Gửi lại cùng một tập nhiều lần là
 * vô hại vì merge phía server là phép hợp (§5.5) — nên không cần buffer, không
 * cần retry riêng.
 */
export function useWatchTracker(
  courseId: number,
  lessonId: number | null,
  videoRef: RefObject<HTMLVideoElement | null>,
  enabled: boolean,
  onSnapshot?: (snapshot: ProgressSnapshot) => void
) {
  const sessionId = useRef<string>(crypto.randomUUID());
  const onSnapshotRef = useRef(onSnapshot);
  onSnapshotRef.current = onSnapshot;

  useEffect(() => {
    sessionId.current = crypto.randomUUID();
  }, [lessonId]);

  useEffect(() => {
    const video = videoRef.current;
    if (!enabled || !lessonId || !video) return;

    const readPlayed = (v: HTMLVideoElement): [number, number][] => {
      const out: [number, number][] = [];
      for (let i = 0; i < v.played.length; i++) {
        out.push([v.played.start(i), v.played.end(i)]);
      }
      return out;
    };

    const send = (keepalive = false) => {
      const v = videoRef.current;
      if (!v || v.played.length === 0) return;
      const payload = {
        positionSeconds: v.currentTime,
        playedRanges: readPlayed(v),
        playbackRate: v.playbackRate,
        clientSessionId: sessionId.current,
        durationSeconds: Math.round(v.duration || 0)
      };
      progressApi
        .sendHeartbeat(courseId, lessonId, payload, keepalive)
        .then((snapshot) => {
          if (snapshot?.lessonId) onSnapshotRef.current?.(snapshot);
        })
        .catch(() => {
          // Payload tích luỹ — mất một heartbeat không mất tiến độ, lần sau gửi lại là đủ.
        });
    };

    const handleFlush = () => send(false);
    const handleVisibility = () => {
      if (document.visibilityState === 'hidden') send(true);
    };
    const handleBeforeUnload = () => send(true);

    const intervalId = window.setInterval(handleFlush, FLUSH_MS);
    video.addEventListener('pause', handleFlush);
    video.addEventListener('ended', handleFlush);
    video.addEventListener('seeked', handleFlush);
    document.addEventListener('visibilitychange', handleVisibility);
    window.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      window.clearInterval(intervalId);
      video.removeEventListener('pause', handleFlush);
      video.removeEventListener('ended', handleFlush);
      video.removeEventListener('seeked', handleFlush);
      document.removeEventListener('visibilitychange', handleVisibility);
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, [courseId, lessonId, enabled, videoRef]);
}
