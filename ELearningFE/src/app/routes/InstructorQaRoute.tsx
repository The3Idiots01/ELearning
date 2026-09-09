import { useNavigate, useParams } from 'react-router-dom';
import { InstructorQaPage } from '../../features/course/pages/instructor/InstructorQaPage';
import { useEffect, useState } from 'react';
import { studentCourseApi } from '../../features/course/api/studentCourseApi';

export function InstructorQaRoute() {
  const { courseId } = useParams();
  const navigate = useNavigate();
  const courseIdNum = Number(courseId);
  const [courseTitle, setCourseTitle] = useState<string>('');

  useEffect(() => {
    if (courseIdNum) {
      studentCourseApi.getCourseDetail(courseIdNum).then((c) => {
        if (c?.title) setCourseTitle(c.title);
      });
    }
  }, [courseIdNum]);

  if (!courseIdNum) return null;

  return (
    <InstructorQaPage
      courseId={courseIdNum}
      courseTitle={courseTitle}
      onBack={() => navigate('/instructor/courses')}
    />
  );
}
