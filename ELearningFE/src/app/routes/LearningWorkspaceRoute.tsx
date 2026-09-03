import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useToast } from '../context/ToastContext';
import { LearningWorkspacePage } from '../../features/course/pages/student/LearningWorkspacePage';
import { studentCourseApi } from '../../features/course/api/studentCourseApi';
import type { CourseDetail, Curriculum } from '../../types/course';

export function LearningWorkspaceRoute() {
  const { courseId } = useParams();
  const courseIdNum = Number(courseId);
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();

  const [courseDetail, setCourseDetail] = useState<CourseDetail | null>(null);
  const [curriculum, setCurriculum] = useState<Curriculum | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);

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
      await studentCourseApi.completeLesson(courseIdNum, lessonId);
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
      showSuccess('Đã lưu tiến độ bài học!');
      return true;
    } catch (err: any) {
      showError(err.message || 'Lỗi khi lưu tiến độ.');
      return false;
    }
  };

  if (!courseIdNum) return null;

  return (
    <LearningWorkspacePage
      courseId={courseIdNum}
      courseDetail={courseDetail}
      curriculum={curriculum}
      isLoading={isLoading}
      onCompleteLesson={handleCompleteLesson}
      onBack={() => navigate('/my-courses')}
    />
  );
}
