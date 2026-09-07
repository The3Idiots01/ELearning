import { apiClient } from '../../../lib/apiClient';
import type { HeartbeatPayload, ProgressSnapshot } from '../../../types/course';

export const progressApi = {
  /**
   * Gửi heartbeat xem video — §5.4 design_us15_us17.md. `keepalive` giữ request
   * sống qua unload/tab ẩn, thay cho navigator.sendBeacon vốn không gửi được
   * header Authorization (heartbeat cần JWT).
   */
  sendHeartbeat: (
    courseId: number,
    lessonId: number,
    payload: HeartbeatPayload,
    keepalive = false
  ): Promise<ProgressSnapshot> =>
    apiClient.post<ProgressSnapshot>(
      `/api/v1/courses/${courseId}/lessons/${lessonId}/progress/heartbeat`,
      payload,
      { keepalive }
    )
};
