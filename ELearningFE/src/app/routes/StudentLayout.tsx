import { useCallback, useEffect, useState } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { StudentNavbar } from '../../components/layout/StudentNavbar';
import { categoryApi } from '../../features/category/api/categoryApi';
import { studentCourseApi } from '../../features/course/api/studentCourseApi';
import type { Category } from '../../types/category';
import type { CourseSummary, EnrolledCourse } from '../../types/course';

export interface StudentOutletContext {
  categories: Category[];
  publicCourses: CourseSummary[];
  enrolledCourses: EnrolledCourse[];
  enrolledCourseIds: Set<number>;
  isStudentLoading: boolean;
  selectedStudentCategoryId: number | null;
  setSelectedStudentCategoryId: (id: number | null) => void;
  selectedStudentLevel: string;
  setSelectedStudentLevel: (level: string) => void;
  studentSearchQuery: string;
  setStudentSearchQuery: (query: string) => void;
  fetchPublicCourses: () => Promise<void>;
  fetchEnrolledCourses: () => Promise<void>;
}

export function StudentLayout() {
  const navigate = useNavigate();

  const [categories, setCategories] = useState<Category[]>([]);
  const [publicCourses, setPublicCourses] = useState<CourseSummary[]>([]);
  const [enrolledCourses, setEnrolledCourses] = useState<EnrolledCourse[]>([]);
  const [selectedStudentCategoryId, setSelectedStudentCategoryId] = useState<number | null>(null);
  const [selectedStudentLevel, setSelectedStudentLevel] = useState<string>('ALL');
  const [studentSearchQuery, setStudentSearchQuery] = useState<string>('');
  const [isStudentLoading, setIsStudentLoading] = useState<boolean>(false);

  const fetchPublicCourses = useCallback(async () => {
    setIsStudentLoading(true);
    try {
      const list = await studentCourseApi.getPublicCourses({
        categoryId: selectedStudentCategoryId,
        level: selectedStudentLevel,
        keyword: studentSearchQuery
      });
      setPublicCourses(list);
    } catch {
      // Handled inside studentCourseApi
    } finally {
      setIsStudentLoading(false);
    }
  }, [selectedStudentCategoryId, selectedStudentLevel, studentSearchQuery]);

  const fetchEnrolledCourses = useCallback(async () => {
    try {
      const list = await studentCourseApi.getEnrolledCourses();
      setEnrolledCourses(list);
    } catch {
      // Handled
    }
  }, []);

  useEffect(() => {
    categoryApi.getAll().then((cats) => setCategories(cats));
    fetchEnrolledCourses();
  }, [fetchEnrolledCourses]);

  useEffect(() => {
    fetchPublicCourses();
  }, [selectedStudentCategoryId, selectedStudentLevel]);

  const enrolledCourseIds = new Set<number>(enrolledCourses.map((c) => c.courseId));

  const context: StudentOutletContext = {
    categories,
    publicCourses,
    enrolledCourses,
    enrolledCourseIds,
    isStudentLoading,
    selectedStudentCategoryId,
    setSelectedStudentCategoryId,
    selectedStudentLevel,
    setSelectedStudentLevel,
    studentSearchQuery,
    setStudentSearchQuery,
    fetchPublicCourses,
    fetchEnrolledCourses
  };

  return (
    <div className="flex-1 flex flex-col min-h-0">
      <StudentNavbar
        searchQuery={studentSearchQuery}
        onSearchChange={setStudentSearchQuery}
        onSearchSubmit={(e) => {
          e.preventDefault();
          navigate('/courses');
          fetchPublicCourses();
        }}
      />
      <Outlet context={context} />
    </div>
  );
}
