import { useNavigate, useOutletContext, useParams } from 'react-router-dom';
import { CourseSettingsPage } from '../../features/course/pages/instructor/CourseSettingsPage';
import type { InstructorOutletContext } from './InstructorLayout';

export function CourseSettingsRoute() {
  const { courseId } = useParams();
  const navigate = useNavigate();
  const { categories } = useOutletContext<InstructorOutletContext>();
  const courseIdNum = Number(courseId);

  if (!courseIdNum) return null;

  return (
    <CourseSettingsPage
      courseId={courseIdNum}
      categories={categories}
      onBack={() => navigate('/instructor/courses')}
      onGoToCurriculum={(id) => navigate(`/instructor/courses/${id}/curriculum`)}
    />
  );
}
