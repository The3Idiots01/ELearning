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

// Auth & Profile Pages
import { LoginPage } from './features/auth/pages/LoginPage';
import { RegisterPage } from './features/auth/pages/RegisterPage';
import { ProfilePage } from './features/auth/pages/ProfilePage';
import { CompleteProfilePage } from './features/auth/pages/CompleteProfilePage';

// Student Pages
import { StudentCatalogPage } from './features/course/pages/student/StudentCatalogPage';
import { CourseDetailPage } from './features/course/pages/student/CourseDetailPage';
import { MyCoursesPage } from './features/course/pages/student/MyCoursesPage';
import { LearningWorkspacePage } from './features/course/pages/student/LearningWorkspacePage';

// Payment Pages & Services
import { paymentApi } from './features/payment/api/paymentApi';
import { PaymentResultPage } from './features/payment/pages/PaymentResultPage';

// Instructor Pages (US-05 & US-06)
import { InstructorCoursesPage } from './features/course/pages/instructor/InstructorCoursesPage';
import { CourseSettingsPage } from './features/course/pages/instructor/CourseSettingsPage';
import { CurriculumEditorPage } from './features/course/pages/instructor/CurriculumEditorPage';

function getInitialAuthRoute(): 'none' | 'login' | 'register' | 'profile' | 'complete-profile' {
  const path = window.location.pathname.toLowerCase();
  const search = new URLSearchParams(window.location.search);
  const pageParam = search.get('page')?.toLowerCase() || search.get('view')?.toLowerCase();
  const hash = window.location.hash.toLowerCase();

  if (path.includes('/login') || pageParam === 'login' || hash === '#login' || hash === '#/login') return 'login';
  if (path.includes('/register') || pageParam === 'register' || hash === '#register' || hash === '#/register') return 'register';
  if (path.includes('/complete-profile') || pageParam === 'complete-profile' || hash === '#complete-profile' || hash === '#/complete-profile') return 'complete-profile';
  if (path.includes('/profile') || pageParam === 'profile' || hash === '#profile' || hash === '#/profile') return 'profile';
  return 'none';
}

function getInitialPaymentParams(): { orderCode: number | null; status: string | null; courseId: number | null } {
  const path = window.location.pathname.toLowerCase();
  const search = new URLSearchParams(window.location.search);
  const orderCodeStr = search.get('orderCode');
  const status = search.get('status');
  const courseIdStr = search.get('courseId');

  if (path.includes('/payment-result') || orderCodeStr) {
    return {
      orderCode: orderCodeStr ? parseInt(orderCodeStr, 10) : null,
      status: status,
      courseId: courseIdStr ? parseInt(courseIdStr, 10) : null
    };
  }
  return { orderCode: null, status: null, courseId: null };
}

function getInitialLearningCourseId(): number | null {
  const path = window.location.pathname.toLowerCase();
  const search = new URLSearchParams(window.location.search);
  const hash = window.location.hash.toLowerCase();

  const courseIdParam = search.get('courseId');
  if (courseIdParam && (path.includes('/learning') || search.get('tab') === 'learning')) {
    const parsed = parseInt(courseIdParam, 10);
    if (!isNaN(parsed) && parsed > 0) return parsed;
  }

  const learningMatch = path.match(/\/learning\/(\d+)/);
  if (learningMatch) {
    const parsed = parseInt(learningMatch[1], 10);
    if (!isNaN(parsed) && parsed > 0) return parsed;
  }

  if (hash.includes('learning')) {
    const hashSearchMatch = hash.match(/courseid=(\d+)/);
    if (hashSearchMatch) {
      const parsed = parseInt(hashSearchMatch[1], 10);
      if (!isNaN(parsed) && parsed > 0) return parsed;
    }
    const hashIdMatch = hash.match(/learning\/(\d+)/);
    if (hashIdMatch) {
      const parsed = parseInt(hashIdMatch[1], 10);
      if (!isNaN(parsed) && parsed > 0) return parsed;
    }
  }

  return null;
}


export function App() {
  const { appMode } = useAuth();
  const { showSuccess, showError } = useToast();

  // Auth routing state for independent testing
  const [authRoute, setAuthRoute] = useState<'none' | 'login' | 'register' | 'profile' | 'complete-profile'>(getInitialAuthRoute());

  // Shared Categories
  const [categories, setCategories] = useState<Category[]>([]);

  // ---------------------------------------------------------------------------
  // STUDENT STATE
  // ---------------------------------------------------------------------------
  const initialPayment = getInitialPaymentParams();
  const initialLearningCourseId = getInitialLearningCourseId();
  const [studentTab, setStudentTab] = useState<'catalog' | 'detail' | 'my-courses' | 'learning' | 'payment-result'>(
    initialPayment.orderCode ? 'payment-result' : initialLearningCourseId ? 'learning' : 'catalog'
  );
  const [paymentOrderCode] = useState<number | null>(initialPayment.orderCode);
  const [paymentStatus] = useState<string | null>(initialPayment.status);
  const [paymentCourseId] = useState<number | null>(initialPayment.courseId);
  const [publicCourses, setPublicCourses] = useState<CourseSummary[]>([]);
  const [enrolledCourses, setEnrolledCourses] = useState<EnrolledCourse[]>([]);
  const [selectedStudentCategoryId, setSelectedStudentCategoryId] = useState<number | null>(null);
  const [selectedStudentLevel, setSelectedStudentLevel] = useState<string>('ALL');
  const [studentSearchQuery, setStudentSearchQuery] = useState<string>('');

  const [selectedStudentCourseId, setSelectedStudentCourseId] = useState<number | null>(initialLearningCourseId);
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

  // Initial Data Load & URL Sync
  useEffect(() => {
    categoryApi.getAll().then((cats) => {
      setCategories(cats);
    });
    fetchPublicCourses();
    fetchEnrolledCourses();

    if (initialLearningCourseId && !initialPayment.orderCode) {
      handleGoToLearning(initialLearningCourseId, true);
    }
  }, []);

  useEffect(() => {
    const handleUrlChange = () => {
      setAuthRoute(getInitialAuthRoute());
      const learningId = getInitialLearningCourseId();
      if (learningId) {
        handleGoToLearning(learningId, true);
      } else if (window.location.pathname === '/' || window.location.pathname === '') {
        const curPayment = getInitialPaymentParams();
        if (!curPayment.orderCode) {
          setStudentTab((prev) => (prev === 'learning' || prev === 'payment-result' ? 'catalog' : prev));
        }
      }
    };
    window.addEventListener('popstate', handleUrlChange);
    window.addEventListener('hashchange', handleUrlChange);
    return () => {
      window.removeEventListener('popstate', handleUrlChange);
      window.removeEventListener('hashchange', handleUrlChange);
    };
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
      const res = await paymentApi.createCheckout(courseId);
      if (res.isEnrolled) {
        showSuccess('🎉 Chúc mừng bạn đã đăng ký khóa học thành công!');
        await fetchEnrolledCourses();
        handleGoToLearning(courseId);
      } else if (res.checkoutUrl) {
        // Redirect student to PayOS payment gateway checkout page
        window.location.href = res.checkoutUrl;
      }
    } catch (err: any) {
      showError(err.message || 'Lỗi khi khởi tạo thanh toán.');
    } finally {
      setIsEnrolling(false);
    }
  };

  const handleGoToLearning = async (courseId: number, replaceUrl: boolean = false) => {
    setSelectedStudentCourseId(courseId);
    setStudentTab('learning');
    setIsStudentLoading(true);

    // Synchronize browser URL to learning workspace
    const targetUrl = `/learning?courseId=${courseId}`;
    if (replaceUrl || window.location.pathname.includes('/payment-result')) {
      window.history.replaceState({ tab: 'learning', courseId }, '', targetUrl);
    } else if (window.location.pathname + window.location.search !== targetUrl) {
      window.history.pushState({ tab: 'learning', courseId }, '', targetUrl);
    }

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

  // ---------------------------------------------------------------------------
  // CONDITIONAL AUTH & PROFILE FLOW RENDERING (TESTABLE VIA URL)
  // ---------------------------------------------------------------------------
  if (authRoute === 'login') {
    return (
      <LoginPage
        onNavigateToRegister={() => setAuthRoute('register')}
        onNavigateToHome={() => setAuthRoute('none')}
        onLoginSuccess={() => setAuthRoute('profile')}
      />
    );
  }

  if (authRoute === 'register') {
    return (
      <RegisterPage
        onNavigateToLogin={() => setAuthRoute('login')}
        onNavigateToHome={() => setAuthRoute('none')}
      />
    );
  }

  if (authRoute === 'complete-profile') {
    return (
      <CompleteProfilePage
        onCompleteSuccess={() => setAuthRoute('profile')}
        onSkip={() => setAuthRoute('none')}
      />
    );
  }

  if (authRoute === 'profile') {
    return (
      <ProfilePage
        onNavigateHome={() => setAuthRoute('none')}
        onNavigateToLogin={() => setAuthRoute('login')}
        onNavigateToCompleteProfile={() => setAuthRoute('complete-profile')}
      />
    );
  }

  return (
    <div className="min-h-screen bg-background text-on-background flex flex-col font-sans">
      {/* Top Demo Banner for switching Roles */}
      <DemoRoleBanner />


      {/* ------------------------------------------------------------------- */}
      {/* 1. STUDENT PORTAL MODE                                              */}
      {/* ------------------------------------------------------------------- */}
      {appMode === 'STUDENT' ? (
        <div className="flex-1 flex flex-col min-h-0">
          {studentTab !== 'learning' && studentTab !== 'payment-result' && (
            <StudentNavbar
              activeTab={studentTab}
              onNavigateTab={(tab) => {
                if (window.location.pathname !== '/' || window.location.search) {
                  window.history.pushState({}, '', '/');
                }
                setStudentTab(tab);
                if (tab === 'catalog') fetchPublicCourses();
                if (tab === 'my-courses') fetchEnrolledCourses();
              }}
              searchQuery={studentSearchQuery}
              onSearchChange={setStudentSearchQuery}
              onSearchSubmit={(e) => {
                e.preventDefault();
                if (window.location.pathname !== '/' || window.location.search) {
                  window.history.pushState({}, '', '/');
                }
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
              onBack={() => {
                if (window.location.pathname !== '/' || window.location.search) {
                  window.history.pushState({}, '', '/');
                }
                setStudentTab('catalog');
              }}
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
              onBack={() => {
                window.history.pushState({}, '', '/');
                setStudentTab('my-courses');
              }}
            />
          )}

          {studentTab === 'payment-result' && (
            <PaymentResultPage
              orderCode={paymentOrderCode}
              initialStatus={paymentStatus}
              initialCourseId={paymentCourseId}
              onGoToLearning={(cId) => {
                fetchEnrolledCourses();
                handleGoToLearning(cId, true);
              }}
              onBackToCatalog={() => {
                window.history.replaceState({}, '', '/');
                fetchEnrolledCourses();
                setStudentTab('catalog');
              }}
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
