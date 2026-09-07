import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useToast } from '../context/ToastContext';
import { LearningWorkspacePage } from '../../features/course/pages/student/LearningWorkspacePage';
import { studentCourseApi } from '../../features/course/api/studentCourseApi';
import type { CourseDetail, Curriculum, ProgressSnapshot } from '../../types/course';

export function LearningWorkspaceRoute() {
  const { courseId } = useParams();
  const courseIdNum = Number(courseId);
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();

  const [courseDetail, setCourseDetail] = useState<CourseDetail | null>(null);
  const [curriculum, setCurriculum] = useState<Curriculum | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [courseProgressPercent, setCourseProgressPercent] = useState<number | null>(null);

  useEffect(() => {
    if (!courseIdNum) return;
    let cancelled = false;
    setIsLoading(true);

    Promise.all([
      studentCourseApi.getCourseDetail(courseIdNum),
      studentCourseApi.getCurriculum(courseIdNum)
    ])
      .then(([detail, curr]) => {
        if (cancelled) return;
        setCourseDetail(detail);
        setCurriculum(curr);
      })
      .catch((err: any) => {
        if (!cancelled) showError(err.message || 'Lỗi khi vào phòng học.');
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [courseIdNum]);

  const handleCompleteLesson = async (lessonId: number): Promise<boolean> => {
    try {
      const response = await studentCourseApi.completeLesson(courseIdNum, lessonId);
      setCurriculum((prev) => {
        if (!prev) return prev;
        const updated = prev.sections.map((sec) => ({
          ...sec,
          lessons: sec.lessons.map((les) =>
            les.id === lessonId ? { ...les, completed: true } : les
          )
        }));
        return { ...prev, sections: updated };
      });
      if (typeof response?.totalProgress === 'number') {
        setCourseProgressPercent(response.totalProgress);
      }
      showSuccess('Đã lưu tiến độ bài học!');
      return true;
    } catch (err: any) {
      showError(err.message || 'Lỗi khi lưu tiến độ.');
      return false;
    }
  };

  // §5.4/§8.2 design_us15_us17.md — mỗi heartbeat cập nhật vị trí resume,
  // coverage và (khi lesson vừa chuyển sang hoàn thành) % course từ server —
  // BR-30, server là nguồn sự thật duy nhất.
  const handleLessonProgress = (snapshot: ProgressSnapshot) => {
    setCurriculum((prev) => {
      if (!prev) return prev;
      const updated = prev.sections.map((sec) => ({
        ...sec,
        lessons: sec.lessons.map((les) =>
          les.id === snapshot.lessonId
            ? {
                ...les,
                lastPositionSeconds: snapshot.lastPositionSeconds,
                coveragePercent: snapshot.coveragePercent,
                completed: les.completed || snapshot.lessonCompleted
              }
            : les
        )
      }));
      return { ...prev, sections: updated };
    });
    if (snapshot.courseProgressPercent != null) {
      setCourseProgressPercent(snapshot.courseProgressPercent);
    }
  };

  if (!courseIdNum) return null;

  return (
    <LearningWorkspacePage
      courseId={courseIdNum}
      courseDetail={courseDetail}
      curriculum={curriculum}
      isLoading={isLoading}
      courseProgressPercent={courseProgressPercent}
      onCompleteLesson={handleCompleteLesson}
      onLessonProgress={handleLessonProgress}
      onBack={() => navigate('/my-courses')}
    />
  );
}
