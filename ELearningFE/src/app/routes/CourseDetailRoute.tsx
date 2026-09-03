import { useEffect, useState } from 'react';
import { useNavigate, useOutletContext, useParams } from 'react-router-dom';
import { useToast } from '../context/ToastContext';
import { CourseDetailPage } from '../../features/course/pages/student/CourseDetailPage';
import { studentCourseApi } from '../../features/course/api/studentCourseApi';
import { paymentApi } from '../../features/payment/api/paymentApi';
import type { CourseDetail, Curriculum } from '../../types/course';
import type { StudentOutletContext } from './StudentLayout';

export function CourseDetailRoute() {
  const { courseId } = useParams();
  const courseIdNum = Number(courseId);
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();
  const { enrolledCourseIds, fetchEnrolledCourses } = useOutletContext<StudentOutletContext>();

  const [course, setCourse] = useState<CourseDetail | null>(null);
  const [curriculum, setCurriculum] = useState<Curriculum | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isEnrolling, setIsEnrolling] = useState<boolean>(false);

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
        setCourse(detail);
        setCurriculum(curr);
      })
      .catch((err: any) => {
        if (!cancelled) showError(err.message || 'Lỗi khi tải chi tiết khóa học.');
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [courseIdNum]);

  const handleEnroll = async (id: number) => {
    setIsEnrolling(true);
    try {
      const res = await paymentApi.createCheckout(id);
      if (res.isEnrolled) {
        showSuccess('🎉 Chúc mừng bạn đã đăng ký khóa học thành công!');
        await fetchEnrolledCourses();
        navigate(`/learning/${id}`);
      } else if (res.checkoutUrl) {
        window.location.href = res.checkoutUrl;
      }
    } catch (err: any) {
      if (
        err?.status === 409 ||
        err?.code === 1213 ||
        err?.message?.toLowerCase()?.includes('already enrolled') ||
        err?.message?.toLowerCase()?.includes('duplicate')
      ) {
        showSuccess('🎉 Bạn đã sở hữu khóa học này rồi! Đang chuyển vào phòng học...');
        await fetchEnrolledCourses();
        navigate(`/learning/${id}`);
      } else {
        showError(err.message || 'Lỗi khi khởi tạo thanh toán.');
      }
    } finally {
      setIsEnrolling(false);
    }
  };

  return (
    <CourseDetailPage
      course={course}
      curriculum={curriculum}
      isEnrolled={enrolledCourseIds.has(courseIdNum)}
      isLoading={isLoading}
      isEnrolling={isEnrolling}
      onEnroll={handleEnroll}
      onGoToLearning={(id) => navigate(`/learning/${id}`)}
      onBack={() => navigate('/courses')}
    />
  );
}
