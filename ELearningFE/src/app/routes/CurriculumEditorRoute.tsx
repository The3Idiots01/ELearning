import { useNavigate, useParams } from 'react-router-dom';
import { CurriculumEditorPage } from '../../features/course/pages/instructor/CurriculumEditorPage';

export function CurriculumEditorRoute() {
  const { courseId } = useParams();
  const navigate = useNavigate();
  const courseIdNum = Number(courseId);

  if (!courseIdNum) return null;

  return (
    <CurriculumEditorPage
      courseId={courseIdNum}
      onBack={() => navigate('/instructor/courses')}
      onGoToSettings={(id) => navigate(`/instructor/courses/${id}/settings`)}
    />
  );
}
