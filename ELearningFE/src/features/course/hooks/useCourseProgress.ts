import { useMemo } from 'react';
import type { Curriculum } from '../../../types/course';

export interface CourseProgressSummary {
  totalUnits: number;
  completedUnits: number;
  totalLessons: number;
  completedLessons: number;
  totalAssessments: number;
  completedAssessments: number;
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
    let totalAssessments = 0;
    let completedAssessments = 0;
    curriculum?.sections.forEach((section) => {
      section.lessons.forEach((lesson) => {
        totalLessons++;
        if (lesson.completed) completedLessons++;
      });
      (section.assessments || []).forEach((assessment) => {
        totalAssessments++;
        if (assessment.completed) completedAssessments++;
      });
    });

    const totalUnits = totalLessons + totalAssessments;
    const completedUnits = completedLessons + completedAssessments;
    const localPercent = totalUnits > 0 ? Math.round((completedUnits / totalUnits) * 100) : 0;
    const percent = serverPercent != null ? Math.round(serverPercent) : localPercent;

    return {
      totalUnits,
      completedUnits,
      totalLessons,
      completedLessons,
      totalAssessments,
      completedAssessments,
      percent
    };
  }, [curriculum, serverPercent]);
}
