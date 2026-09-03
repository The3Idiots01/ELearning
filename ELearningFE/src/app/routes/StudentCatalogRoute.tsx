import { useNavigate, useOutletContext } from 'react-router-dom';
import { StudentCatalogPage } from '../../features/course/pages/student/StudentCatalogPage';
import type { CourseSummary } from '../../types/course';
import type { StudentOutletContext } from './StudentLayout';

export function StudentCatalogRoute() {
  const navigate = useNavigate();
  const {
    publicCourses,
    categories,
    selectedStudentCategoryId,
    setSelectedStudentCategoryId,
    selectedStudentLevel,
    setSelectedStudentLevel,
    studentSearchQuery,
    setStudentSearchQuery,
    isStudentLoading,
    enrolledCourseIds
  } = useOutletContext<StudentOutletContext>();

  return (
    <StudentCatalogPage
      courses={publicCourses}
      categories={categories}
      selectedCategoryId={selectedStudentCategoryId}
      onSelectCategory={setSelectedStudentCategoryId}
      selectedLevel={selectedStudentLevel}
      onSelectLevel={setSelectedStudentLevel}
      searchQuery={studentSearchQuery}
      onResetFilters={() => {
        setSelectedStudentCategoryId(null);
        setSelectedStudentLevel('ALL');
        setStudentSearchQuery('');
      }}
      isLoading={isStudentLoading}
      onSelectCourse={(course: CourseSummary) => navigate(`/courses/${course.id}`)}
      enrolledCourseIds={enrolledCourseIds}
    />
  );
}
