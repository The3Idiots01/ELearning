import { useMemo } from 'react';
import type { Curriculum } from '../../../types/course';

export interface CourseProgressSummary {
  totalLessons: number;
  completedLessons: number;
  percent: number;
}

/**
 * Tiến độ toàn course cho sidebar/header — §8.1 design_us15_us17.md. Cờ
 * `completed` trong curriculum đã tới từ server (LessonResponseAssembler);
 * `serverPercent` (courseProgressPercent trả về từ heartbeat/complete) là
 * nguồn sự thật ưu tiên khi có, vì rollup chỉ tính lại đúng lúc lesson đổi
 * trạng thái (BR-30) — tránh lệch số giữa hai chỗ hiển thị.
 */
export function useCourseProgress(
  curriculum: Curriculum | null,
  serverPercent?: number | null
): CourseProgressSummary {
  return useMemo(() => {
    let totalLessons = 0;
    let completedLessons = 0;
    curriculum?.sections.forEach((section) => {
      section.lessons.forEach((lesson) => {
        totalLessons++;
        if (lesson.completed) completedLessons++;
      });
    });

    const localPercent = totalLessons > 0 ? Math.round((completedLessons / totalLessons) * 100) : 0;
    const percent = serverPercent != null ? Math.round(serverPercent) : localPercent;

    return { totalLessons, completedLessons, percent };
  }, [curriculum, serverPercent]);
}
