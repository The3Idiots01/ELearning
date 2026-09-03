import { useNavigate, useOutletContext } from 'react-router-dom';
import { MyCoursesPage } from '../../features/course/pages/student/MyCoursesPage';
import type { StudentOutletContext } from './StudentLayout';

export function MyCoursesRoute() {
  const navigate = useNavigate();
  const { enrolledCourses, isStudentLoading } = useOutletContext<StudentOutletContext>();

  return (
    <MyCoursesPage
      enrolledCourses={enrolledCourses}
      isLoading={isStudentLoading}
      onGoToLearning={(courseId) => navigate(`/learning/${courseId}`)}
      onExploreMore={() => navigate('/courses')}
    />
  );
}
