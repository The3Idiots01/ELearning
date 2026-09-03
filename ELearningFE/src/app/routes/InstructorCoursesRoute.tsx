import { useNavigate, useOutletContext } from 'react-router-dom';
import { InstructorCoursesPage } from '../../features/course/pages/instructor/InstructorCoursesPage';
import type { InstructorOutletContext } from './InstructorLayout';

export function InstructorCoursesRoute() {
  const navigate = useNavigate();
  const { categories } = useOutletContext<InstructorOutletContext>();

  return (
    <InstructorCoursesPage
      categories={categories}
      onEditCourse={(courseId) => navigate(`/instructor/courses/${courseId}/settings`)}
      onEditCurriculum={(courseId) => navigate(`/instructor/courses/${courseId}/curriculum`)}
    />
  );
}
