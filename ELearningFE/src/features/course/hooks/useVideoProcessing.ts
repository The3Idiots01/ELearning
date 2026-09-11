import { useEffect, useRef, useState, type Dispatch, type SetStateAction } from 'react';
import type { Curriculum } from '../../../types/course';
import { videoProcessingApi } from '../api/videoProcessingApi';

export function useVideoProcessing(courseId: number, curriculum: Curriculum | null,
  setCurriculum: Dispatch<SetStateAction<Curriculum | null>>) {
  const [error, setError] = useState('');
  const loaded = curriculum !== null;
  const pending = curriculum?.sections.some(s => s.lessons.some(l => l.uploadStatus === 'PROCESSING')) ?? false;
  const previousStatuses = useRef('');
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    if (!loaded) return;
    let stopped = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let inFlight = false;
    const controller = new AbortController();
    const poll = async () => {
      if (stopped || inFlight || document.visibilityState === 'hidden') return;
      inFlight = true;
      try {
        const statuses = await videoProcessingApi.list(courseId, controller.signal);
        if (stopped) return;
        setError('');
        const signature = JSON.stringify(statuses.map(s => [s.lessonId, s.uploadStatus, s.durationSeconds, s.errorCode]));
        if (signature !== previousStatuses.current) {
          previousStatuses.current = signature; setRevision(r => r + 1);
        }
        const byId = new Map(statuses.map(s => [s.lessonId, s]));
        setCurriculum(current => {
          if (!current || current.courseId !== courseId) return current;
          let changed = false;
          const sections = current.sections.map(section => ({ ...section, lessons: section.lessons.map(lesson => {
            const status = byId.get(lesson.id);
            if (!status || (lesson.uploadStatus === status.uploadStatus && lesson.durationSeconds === status.durationSeconds
                && lesson.processingErrorCode === status.errorCode && lesson.canRetryProcessing === status.canRetry)) return lesson;
            changed = true;
            return { ...lesson, uploadStatus: status.uploadStatus, durationSeconds: status.durationSeconds,
              processingErrorCode: status.errorCode, canRetryProcessing: status.canRetry, playable: status.uploadStatus === 'READY' };
          }) }));
          return changed ? { ...current, sections } : current;
        });
      } catch {
        if (!stopped) setError('Không thể cập nhật trạng thái video. Đang thử kết nối lại.');
      } finally {
        inFlight = false;
        if (!stopped && pending) timer = setTimeout(() => void poll(), 3000);
      }
    };
    const visible = () => { if (document.visibilityState === 'visible') { clearTimeout(timer); void poll(); } };
    document.addEventListener('visibilitychange', visible);
    void poll();
    return () => { stopped = true; controller.abort(); clearTimeout(timer); document.removeEventListener('visibilitychange', visible); };
  }, [courseId, loaded, pending, setCurriculum]);
  return { error, revision };
}
