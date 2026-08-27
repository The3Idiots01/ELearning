import { useState, useEffect } from 'react';
import { useAuth } from './app/context/AuthContext';
import { useToast } from './app/context/ToastContext';
import { categoryApi } from './features/category/api/categoryApi';
import { studentCourseApi } from './features/course/api/studentCourseApi';
import type { Category } from './types/category';
import type { CourseSummary, CourseDetail, Curriculum, EnrolledCourse } from './types/course';

// Layout Components
import { DemoRoleBanner } from './components/layout/DemoRoleBanner';
import { StudentNavbar } from './components/layout/StudentNavbar';
import { InstructorSidebar } from './components/layout/InstructorSidebar';

// Student Pages
import { StudentCatalogPage } from './features/course/pages/student/StudentCatalogPage';
import { CourseDetailPage } from './features/course/pages/student/CourseDetailPage';
import { MyCoursesPage } from './features/course/pages/student/MyCoursesPage';
import { LearningWorkspacePage } from './features/course/pages/student/LearningWorkspacePage';

// Instructor Pages (US-05 & US-06)
import { InstructorCoursesPage } from './features/course/pages/instructor/InstructorCoursesPage';
import { CourseSettingsPage } from './features/course/pages/instructor/CourseSettingsPage';
import { CurriculumEditorPage } from './features/course/pages/instructor/CurriculumEditorPage';

export function App() {
  const { appMode } = useAuth();
  const { showSuccess, showError } = useToast();

  // Shared Categories
  const [categories, setCategories] = useState<Category[]>([]);

  // ---------------------------------------------------------------------------
  // STUDENT STATE
  // ---------------------------------------------------------------------------
  const [studentTab, setStudentTab] = useState<'catalog' | 'detail' | 'my-courses' | 'learning'>('catalog');
  const [publicCourses, setPublicCourses] = useState<CourseSummary[]>([]);
  const [enrolledCourses, setEnrolledCourses] = useState<EnrolledCourse[]>([]);
  const [selectedStudentCategoryId, setSelectedStudentCategoryId] = useState<number | null>(null);
  const [selectedStudentLevel, setSelectedStudentLevel] = useState<string>('ALL');
  const [studentSearchQuery, setStudentSearchQuery] = useState<string>('');

  const [selectedStudentCourseId, setSelectedStudentCourseId] = useState<number | null>(null);
  const [studentCourseDetail, setStudentCourseDetail] = useState<CourseDetail | null>(null);
  const [studentCurriculum, setStudentCurriculum] = useState<Curriculum | null>(null);
  const [isStudentLoading, setIsStudentLoading] = useState<boolean>(false);
  const [isEnrolling, setIsEnrolling] = useState<boolean>(false);

  // ---------------------------------------------------------------------------
  // INSTRUCTOR STATE (US-05 & US-06)
  // ---------------------------------------------------------------------------
  const [instructorView, setInstructorView] = useState<'courses' | 'course-settings' | 'curriculum'>('courses');
  const [selectedInstructorCourseId, setSelectedInstructorCourseId] = useState<number | null>(null);
  const [selectedInstructorCourseTitle, setSelectedInstructorCourseTitle] = useState<string>('');

  // Initial Data Load
  useEffect(() => {
    categoryApi.getAll().then((cats) => {
      setCategories(cats);
    });
    fetchPublicCourses();
    fetchEnrolledCourses();
  }, []);

  // ---------------------------------------------------------------------------
  // STUDENT HANDLERS
  // ---------------------------------------------------------------------------
  const fetchPublicCourses = async () => {
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
  };

  useEffect(() => {
    if (appMode === 'STUDENT') {
      fetchPublicCourses();
    }
  }, [selectedStudentCategoryId, selectedStudentLevel, appMode]);

  const fetchEnrolledCourses = async () => {
    try {
      const list = await studentCourseApi.getEnrolledCourses();
      setEnrolledCourses(list);
    } catch {
      // Handled
    }
  };

  const handleSelectStudentCourse = async (course: CourseSummary) => {
    setSelectedStudentCourseId(course.id);
    setStudentTab('detail');
    setIsStudentLoading(true);

    try {
      const [detail, curr] = await Promise.all([
        studentCourseApi.getCourseDetail(course.id),
        studentCourseApi.getCurriculum(course.id)
      ]);
      setStudentCourseDetail(detail);
      setStudentCurriculum(curr);
    } catch (err: any) {
      showError(err.message || 'Lỗi khi tải chi tiết khóa học.');
    } finally {
      setIsStudentLoading(false);
    }
  };

  const handleEnrollCourse = async (courseId: number) => {
    setIsEnrolling(true);
    try {
      await studentCourseApi.enrollCourse(courseId);
      showSuccess('🎉 Chúc mừng bạn đã đăng ký khóa học thành công!');
      await fetchEnrolledCourses();
      handleGoToLearning(courseId);
    } catch (err: any) {
      showError(err.message || 'Lỗi khi đăng ký khóa học.');
    } finally {
      setIsEnrolling(false);
    }
  };

  const handleGoToLearning = async (courseId: number) => {
    setSelectedStudentCourseId(courseId);
    setStudentTab('learning');
    setIsStudentLoading(true);

    try {
      const [detail, curr] = await Promise.all([
        studentCourseApi.getCourseDetail(courseId),
        studentCourseApi.getCurriculum(courseId)
      ]);
      setStudentCourseDetail(detail);
      setStudentCurriculum(curr);
    } catch (err: any) {
      showError(err.message || 'Lỗi khi vào phòng học.');
    } finally {
      setIsStudentLoading(false);
    }
  };

  const handleCompleteLesson = async (lessonId: number): Promise<boolean> => {
    if (!selectedStudentCourseId) return false;
    try {
      await studentCourseApi.completeLesson(selectedStudentCourseId, lessonId);
      // Update locally
      setStudentCurriculum((prev) => {
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

  const enrolledCourseIds = new Set<number>(enrolledCourses.map((c) => c.courseId));

  // ---------------------------------------------------------------------------
  // INSTRUCTOR HANDLERS
  // ---------------------------------------------------------------------------
  const handleEditInstructorCourse = (courseId: number) => {
    setSelectedInstructorCourseId(courseId);
    setInstructorView('course-settings');
  };

  const handleEditInstructorCurriculum = (courseId: number) => {
    setSelectedInstructorCourseId(courseId);
    setInstructorView('curriculum');
  };

  return (
    <div className="min-h-screen bg-background text-on-background flex flex-col font-sans">
      {/* Top Demo Banner for switching Roles */}
      <DemoRoleBanner />

      {/* ------------------------------------------------------------------- */}
      {/* 1. STUDENT PORTAL MODE                                              */}
      {/* ------------------------------------------------------------------- */}
      {appMode === 'STUDENT' ? (
        <div className="flex-1 flex flex-col min-h-0">
          {studentTab !== 'learning' && (
            <StudentNavbar
              activeTab={studentTab}
              onNavigateTab={(tab) => {
                setStudentTab(tab);
                if (tab === 'catalog') fetchPublicCourses();
                if (tab === 'my-courses') fetchEnrolledCourses();
              }}
              searchQuery={studentSearchQuery}
              onSearchChange={setStudentSearchQuery}
              onSearchSubmit={(e) => {
                e.preventDefault();
                setStudentTab('catalog');
                fetchPublicCourses();
              }}
            />
          )}

          {studentTab === 'catalog' && (
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
              onSelectCourse={handleSelectStudentCourse}
              enrolledCourseIds={enrolledCourseIds}
            />
          )}

          {studentTab === 'detail' && (
            <CourseDetailPage
              course={studentCourseDetail}
              curriculum={studentCurriculum}
              isEnrolled={selectedStudentCourseId ? enrolledCourseIds.has(selectedStudentCourseId) : false}
              isLoading={isStudentLoading}
              isEnrolling={isEnrolling}
              onEnroll={handleEnrollCourse}
              onGoToLearning={handleGoToLearning}
              onBack={() => setStudentTab('catalog')}
            />
          )}

          {studentTab === 'my-courses' && (
            <MyCoursesPage
              enrolledCourses={enrolledCourses}
              isLoading={isStudentLoading}
              onGoToLearning={handleGoToLearning}
              onExploreMore={() => setStudentTab('catalog')}
            />
          )}

          {studentTab === 'learning' && selectedStudentCourseId && (
            <LearningWorkspacePage
              courseId={selectedStudentCourseId}
              courseDetail={studentCourseDetail}
              curriculum={studentCurriculum}
              isLoading={isStudentLoading}
              onCompleteLesson={handleCompleteLesson}
              onBack={() => setStudentTab('my-courses')}
            />
          )}
        </div>
      ) : (
        /* ----------------------------------------------------------------- */
        /* 2. INSTRUCTOR STUDIO MODE (US-05 & US-06)                         */
        /* ----------------------------------------------------------------- */
        <div className="flex-1 flex h-[calc(100vh-36px)] overflow-hidden">
          {/* Side Navbar */}
          <InstructorSidebar
            activeView={instructorView}
            onNavigate={(view) => {
              setInstructorView(view);
              setSelectedInstructorCourseId(null);
              setSelectedInstructorCourseTitle('');
            }}
            selectedCourseTitle={selectedInstructorCourseTitle}
          />

          {/* Main Studio Area */}
          <main className="flex-1 flex flex-col bg-background overflow-hidden min-h-0">
            {instructorView === 'courses' && (
              <InstructorCoursesPage
                categories={categories}
                onEditCourse={handleEditInstructorCourse}
                onEditCurriculum={handleEditInstructorCurriculum}
              />
            )}

            {instructorView === 'course-settings' && selectedInstructorCourseId && (
              <CourseSettingsPage
                courseId={selectedInstructorCourseId}
                categories={categories}
                onBack={() => setInstructorView('courses')}
                onGoToCurriculum={handleEditInstructorCurriculum}
              />
            )}

            {instructorView === 'curriculum' && selectedInstructorCourseId && (
              <CurriculumEditorPage
                courseId={selectedInstructorCourseId}
                onBack={() => setInstructorView('courses')}
                onGoToSettings={handleEditInstructorCourse}
              />
            )}
          </main>
        </div>
      )}
    </div>
  );
}

export default App;
