import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './app/context/AuthContext';

// Route guards
import { RequireAuth, RedirectIfAuthed } from './app/routes/guards';

// Auth & Profile Routes
import { LoginRoute } from './app/routes/LoginRoute';
import { RegisterRoute } from './app/routes/RegisterRoute';
import { ProfileRoute } from './app/routes/ProfileRoute';
import { CompleteProfileRoute } from './app/routes/CompleteProfileRoute';

// Student Routes
import { StudentLayout } from './app/routes/StudentLayout';
import { StudentCatalogRoute } from './app/routes/StudentCatalogRoute';
import { CourseDetailRoute } from './app/routes/CourseDetailRoute';
import { MyCoursesRoute } from './app/routes/MyCoursesRoute';
import { LearningWorkspaceRoute } from './app/routes/LearningWorkspaceRoute';

// Instructor Routes (US-05 & US-06)
import { InstructorLayout } from './app/routes/InstructorLayout';
import { InstructorCoursesRoute } from './app/routes/InstructorCoursesRoute';
import { CourseSettingsRoute } from './app/routes/CourseSettingsRoute';
import { CurriculumEditorRoute } from './app/routes/CurriculumEditorRoute';

export function App() {
  const { appMode } = useAuth();
  const defaultPath = appMode === 'LECTURER' ? '/instructor/courses' : '/courses';

  return (
    <div className="min-h-screen bg-background text-on-background flex flex-col font-sans">
      <Routes>
        {/* Auth & Profile flows — real URLs, guarded by actual auth state */}
        <Route
          path="/login"
          element={
            <RedirectIfAuthed>
              <LoginRoute />
            </RedirectIfAuthed>
          }
        />
        <Route
          path="/register"
          element={
            <RedirectIfAuthed>
              <RegisterRoute />
            </RedirectIfAuthed>
          }
        />
        <Route
          path="/complete-profile"
          element={
            <RequireAuth>
              <CompleteProfileRoute />
            </RequireAuth>
          }
        />
        <Route
          path="/profile"
          element={
            <RequireAuth>
              <ProfileRoute />
            </RequireAuth>
          }
        />

        {/* Student Portal */}
        <Route element={<StudentLayout />}>
          <Route path="/courses" element={<StudentCatalogRoute />} />
          <Route path="/courses/:courseId" element={<CourseDetailRoute />} />
          <Route path="/my-courses" element={<MyCoursesRoute />} />
        </Route>
        <Route path="/learning/:courseId" element={<LearningWorkspaceRoute />} />

        {/* Instructor Studio (US-05 & US-06) */}
        <Route
          element={
            <RequireAuth>
              <InstructorLayout />
            </RequireAuth>
          }
        >
          <Route path="/instructor/courses" element={<InstructorCoursesRoute />} />
          <Route path="/instructor/courses/:courseId/settings" element={<CourseSettingsRoute />} />
          <Route path="/instructor/courses/:courseId/curriculum" element={<CurriculumEditorRoute />} />
        </Route>

        <Route path="/" element={<Navigate to={defaultPath} replace />} />
        <Route path="*" element={<Navigate to={defaultPath} replace />} />
      </Routes>
    </div>
  );
}

export default App;
