import { useCallback, useEffect, useRef, useState } from 'react';
import type { Lesson, ProgressSnapshot } from '../../../types/course';
import { usePlaybackTicket } from '../hooks/usePlaybackTicket';
import { useWatchTracker } from '../hooks/useWatchTracker';

export function LessonVideoPlayer({ courseId, lesson, onProgress }: {
  courseId: number; lesson: Lesson; onProgress?: (snapshot: ProgressSnapshot) => void;
}) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [videoMounted, setVideoMounted] = useState(false);
  const [trackingReady, setTrackingReady] = useState(false);
  const [atEnd, setAtEnd] = useState(false);
  const [playError, setPlayError] = useState('');
  const initialPosition = useRef(lesson.lastPositionSeconds || 0);
  const loadedOnce = useRef(false);
  const renewalAttempted = useRef(false);
  const savedPlayback = useRef({ position: initialPosition.current, paused: false, rate: 1 });
  const enabled = Boolean(lesson.playable);
  const { ticket, isLoading, error, isForbidden, renew } = usePlaybackTicket(courseId, lesson.id, enabled);
  const video = videoRef.current;
  const syncError = useWatchTracker(courseId, lesson.id, video, enabled && trackingReady, onProgress);
  const setVideo = useCallback((node: HTMLVideoElement | null) => {
    videoRef.current = node;
    setVideoMounted(Boolean(node));
  }, []);
  useEffect(() => {
    const element = videoRef.current;
    if (!element || !ticket) return;
    setTrackingReady(false);
    const previous = loadedOnce.current ? { position: element.currentTime, paused: element.paused, rate: element.playbackRate } : savedPlayback.current;
    savedPlayback.current = previous;
    const restore = () => {
      if (!Number.isFinite(element.duration) || element.duration <= 0) return;
      const position = Math.max(0, Math.min(previous.position, element.duration));
      const ended = position >= element.duration - 1;
      element.currentTime = position;
      element.playbackRate = previous.rate;
      loadedOnce.current = true;
      setTrackingReady(true); setAtEnd(ended); setPlayError('');
      if (!previous.paused && !ended) void element.play().catch(() => { /* Browser may require the learner to press play. */ });
    };
    element.addEventListener('loadedmetadata', restore, { once: true });
    element.src = ticket.streamUrl;
    element.load();
    return () => { element.removeEventListener('loadedmetadata', restore); };
  }, [videoMounted, ticket]);
  const handleError = async () => {
    if (renewalAttempted.current) { setPlayError('Không thể phát video. Vui lòng thử lại sau.'); return; }
    renewalAttempted.current = true;
    await renew();
  };
  const replay = () => { if (!video) return; setAtEnd(false); video.currentTime = 0; void video.play().catch(() => {}); };
  if (!enabled || isForbidden) return <p className="p-8 text-slate-300">{isForbidden ? 'Bạn không còn quyền xem nội dung này.' : 'Nội dung chưa sẵn sàng để phát.'}</p>;
  if (!ticket) return <p className="p-8 text-slate-300">{error || (isLoading ? 'Đang tải video bài giảng...' : 'Đang chuẩn bị video...')}</p>;
  return <div className="w-full h-full relative">
    <video ref={setVideo} controls onError={() => void handleError()} onEnded={() => setAtEnd(true)}
      className="w-full h-full max-h-[60vh] object-contain" />
    {atEnd && <div className="absolute inset-x-0 top-3 flex justify-center"><button type="button" onClick={replay} className="bg-slate-900 text-white rounded-lg px-4 py-2">Đã xem hết — Xem lại từ đầu</button></div>}
    {(syncError || error || playError) && <p role="status" className="absolute bottom-14 inset-x-3 bg-slate-900/90 text-amber-200 rounded-lg p-2 text-sm">
      {playError || error || 'Chưa đồng bộ được tiến độ. Hệ thống sẽ thử lại khi kết nối phục hồi.'}</p>}
  </div>;
}
