import { apiClient, API_BASE_URL } from '../../../lib/apiClient';
import type { PlaybackTicket } from '../../../types/course';

export const playbackApi = {
  /**
   * Cấp PlaybackTicket cho một lesson — §4.6 design_us15_us17.md.
   * streamUrl trả về là đường dẫn tương đối; dựng thành URL tuyệt đối để gán
   * thẳng vào <video src>, vì thẻ <video> không đi qua apiClient.
   */
  getPlaybackTicket: async (courseId: number, lessonId: number): Promise<PlaybackTicket> => {
    const ticket = await apiClient.get<PlaybackTicket>(
      `/api/v1/courses/${courseId}/lessons/${lessonId}/playback`,
      { credentials: 'include' } // §4.5/§8.3 — cần nhận Set-Cookie lv_pb
    );
    return { ...ticket, streamUrl: `${API_BASE_URL}${ticket.streamUrl}` };
  }
};
