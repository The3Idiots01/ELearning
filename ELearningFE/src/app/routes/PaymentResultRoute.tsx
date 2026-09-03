import { useNavigate, useOutletContext, useSearchParams } from 'react-router-dom';
import { PaymentResultPage } from '../../features/payment/pages/PaymentResultPage';
import type { StudentOutletContext } from './StudentLayout';

export function PaymentResultRoute() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { fetchEnrolledCourses } = useOutletContext<StudentOutletContext>();

  const orderCodeParam = searchParams.get('orderCode');
  const orderCode = orderCodeParam ? parseInt(orderCodeParam, 10) : null;
  const status = searchParams.get('status');
  const courseIdParam = searchParams.get('courseId');
  const initialCourseId = courseIdParam ? parseInt(courseIdParam, 10) : null;

  const handleGoToLearning = async (courseId: number) => {
    await fetchEnrolledCourses();
    navigate(`/learning/${courseId}`);
  };

  const handleBackToCatalog = async () => {
    await fetchEnrolledCourses();
    navigate('/courses');
  };

  return (
    <PaymentResultPage
      orderCode={orderCode}
      initialStatus={status}
      initialCourseId={initialCourseId}
      onGoToLearning={handleGoToLearning}
      onBackToCatalog={handleBackToCatalog}
    />
  );
}
