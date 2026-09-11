import { useEffect, useRef, useState } from 'react';
import { progressApi } from '../api/progressApi';
import type { HeartbeatPayload, ProgressSnapshot } from '../../../types/course';

/** Bind to a concrete media element: cleanup must never read the next lesson's ref. */
export function useWatchTracker(courseId: number, lessonId: number, video: HTMLVideoElement | null,
  enabled: boolean, onSnapshot?: (snapshot: ProgressSnapshot) => void) {
  const callback = useRef(onSnapshot);
  useEffect(() => { callback.current = onSnapshot; }, [onSnapshot]);
  const [syncError, setSyncError] = useState(false);
  useEffect(() => {
    if (!enabled || !video) return;
    const sessionId = crypto.randomUUID();
    let disposed = false;
    let inFlight = false;
    let failures = 0;
    let pending: HeartbeatPayload | null = null;
    let last: HeartbeatPayload | null = null;
    let acknowledged = '';
    let sending = '';
    let interacted = video.played.length > 0;
    const capture = () => {
      if (video.readyState < 1 || !Number.isFinite(video.currentTime) || !Number.isFinite(video.duration)) return last;
      if (!interacted && video.played.length === 0) return last;
      const ranges: [number, number][] = [];
      for (let i = 0; i < video.played.length; i++) ranges.push([video.played.start(i), video.played.end(i)]);
      last = { positionSeconds: video.currentTime, playedRanges: ranges, playbackRate: video.playbackRate,
        clientSessionId: sessionId, durationSeconds: Math.round(video.duration) };
      return last;
    };
    const send = (payload: HeartbeatPayload, keepalive = false) => {
      const signature = JSON.stringify(payload);
      if (signature === acknowledged || signature === sending) return;
      if (inFlight && !keepalive) { pending = payload; return; }
      inFlight = true; sending = signature;
      void progressApi.sendHeartbeat(courseId, lessonId, payload, keepalive).then(snapshot => {
        acknowledged = signature; failures = 0;
        if (!disposed) { setSyncError(false); if (snapshot?.lessonId === lessonId) callback.current?.(snapshot); }
      }).catch(() => {
        failures++;
        if (!disposed) { pending = last || payload; if (failures >= 2) setSyncError(true); }
      }).finally(() => {
        inFlight = false; sending = '';
        if (pending && !disposed && failures === 0) { const next = pending; pending = null; send(next); }
      });
    };
    const flush = (keepalive = false) => { const value = capture(); if (value) send(value, keepalive); };
    const played = () => { interacted = true; capture(); };
    const seeked = () => { interacted = true; flush(); };
    const normal = () => flush();
    const leaving = () => flush(true);
    const visibility = () => { if (document.visibilityState === 'hidden') leaving(); };
    const online = () => flush();
    const interval = window.setInterval(normal, 10_000);
    video.addEventListener('timeupdate', played);
    video.addEventListener('pause', normal);
    video.addEventListener('ended', normal);
    video.addEventListener('seeked', seeked);
    document.addEventListener('visibilitychange', visibility);
    window.addEventListener('pagehide', leaving);
    window.addEventListener('online', online);
    return () => {
      // last captures the pre-detach position even if readyState is already reset by the browser.
      leaving(); disposed = true;
      clearInterval(interval);
      video.removeEventListener('timeupdate', played);
      video.removeEventListener('pause', normal);
      video.removeEventListener('ended', normal);
      video.removeEventListener('seeked', seeked);
      document.removeEventListener('visibilitychange', visibility);
      window.removeEventListener('pagehide', leaving);
      window.removeEventListener('online', online);
    };
  }, [courseId, lessonId, video, enabled]);
  return syncError;
}
