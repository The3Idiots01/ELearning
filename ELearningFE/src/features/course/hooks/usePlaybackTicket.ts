import { useCallback, useEffect, useRef, useState } from 'react';
import { playbackApi } from '../api/playbackApi';
import { ApiError } from '../../../lib/apiClient';
import type { PlaybackTicket } from '../../../types/course';

const RENEW_BEFORE_EXPIRY_MS = 5 * 60 * 1000;

interface UsePlaybackTicketResult {
  ticket: PlaybackTicket | null;
  isLoading: boolean;
  error: string | null;
  /** true khi quyền đã bị thu hồi thật (403) — không nên tự động gia hạn lặp lại (§4.8). */
  isForbidden: boolean;
  renew: () => Promise<PlaybackTicket | null>;
}

/**
 * Cấp + tự gia hạn PlaybackTicket cho một lesson — §8.3 design_us15_us17.md.
 * Với ticket 8 giờ, luồng bình thường không gia hạn lần nào; renew() chỉ được
 * gọi từ đường phản ứng: <video onError>, hoặc tab quay lại foreground khi
 * ticket sắp hết hạn.
 */
export function usePlaybackTicket(
  courseId: number,
  lessonId: number | null,
  enabled: boolean
): UsePlaybackTicketResult {
  const [ticket, setTicket] = useState<PlaybackTicket | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isForbidden, setIsForbidden] = useState(false);
  const requestSeq = useRef(0);

  const fetchTicket = useCallback(async (): Promise<PlaybackTicket | null> => {
    if (!lessonId) return null;
    const seq = ++requestSeq.current;
    setIsLoading(true);
    setError(null);
    try {
      const next = await playbackApi.getPlaybackTicket(courseId, lessonId);
      if (seq !== requestSeq.current) return null;
      setTicket(next);
      setIsForbidden(false);
      return next;
    } catch (err) {
      if (seq !== requestSeq.current) return null;
      const message =
        err instanceof ApiError ? err.message : 'Không thể tải video bài giảng. Vui lòng thử lại.';
      setError(message);
      setIsForbidden(err instanceof ApiError && err.status === 403);
      return null;
    } finally {
      if (seq === requestSeq.current) setIsLoading(false);
    }
  }, [courseId, lessonId]);

  useEffect(() => {
    setTicket(null);
    setError(null);
    setIsForbidden(false);
    if (!enabled || !lessonId) return;
    void fetchTicket();
    return () => { requestSeq.current++; };
  }, [enabled, lessonId, fetchTicket]);

  useEffect(() => {
    if (!enabled || !ticket) return;
    const handleVisibility = () => {
      if (document.visibilityState !== 'visible') return;
      const expiresAt = new Date(ticket.expiresAt).getTime();
      if (expiresAt - Date.now() < RENEW_BEFORE_EXPIRY_MS) {
        void fetchTicket();
      }
    };
    document.addEventListener('visibilitychange', handleVisibility);
    return () => document.removeEventListener('visibilitychange', handleVisibility);
  }, [enabled, ticket, fetchTicket]);

  return { ticket, isLoading, error, isForbidden, renew: fetchTicket };
}
