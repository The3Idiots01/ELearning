import React, { useEffect, useState } from 'react';
import { useAuth } from '../../../../app/context/AuthContext';
import { useToast } from '../../../../app/context/ToastContext';
import { LevelBadge, StatusBadge } from '../../../../components/common/Badge';
import { ConfirmDialog } from '../../../../components/common/ConfirmDialog';
import { Modal } from '../../../../components/common/Modal';
import { formatCurrencyVND, formatDate } from '../../../../lib/formatters';
import type { Category } from '../../../../types/category';
import type { CourseStatus, CourseSummary } from '../../../../types/course';
import { instructorCourseApi } from '../../api/instructorCourseApi';
import { PublishCheckModal } from '../../components/PublishCheckModal';
import { StatusLogsModal } from '../../components/StatusLogsModal';

interface InstructorCoursesPageProps {
  categories: Category[];
  onEditCourse: (courseId: number) => void;
  onEditCurriculum: (courseId: number) => void;
}

export const InstructorCoursesPage: React.FC<InstructorCoursesPageProps> = ({
  categories,
  onEditCourse,
  onEditCurriculum
}) => {
  const { ensureInstructorToken } = useAuth();
  const { showSuccess, showError } = useToast();

  const [courses, setCourses] = useState<CourseSummary[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [filterStatus, setFilterStatus] = useState<CourseStatus | ''>('');
  const [searchKeyword, setSearchKeyword] = useState('');

  // Create Course Modal
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newCategoryId, setNewCategoryId] = useState<number>(categories[0]?.id || 1);
  const [isCreating, setIsCreating] = useState(false);

  // Publish Check Modal
  const [publishCheckCourseId, setPublishCheckCourseId] = useState<number | null>(null);

  // Status Logs Modal
  const [statusLogsCourseId, setStatusLogsCourseId] = useState<number | null>(null);

  // Unpublish Modal
  const [unpublishCourseId, setUnpublishCourseId] = useState<number | null>(null);
  const [unpublishReason, setUnpublishReason] = useState('');
  const [isUnpublishing, setIsUnpublishing] = useState(false);

  // Delete Dialog
  const [deleteCourseId, setDeleteCourseId] = useState<number | null>(null);

  const fetchCourses = async () => {
    setIsLoading(true);
    try {
      await ensureInstructorToken();
      const res = await instructorCourseApi.listMine({
        status: filterStatus,
        keyword: searchKeyword
      });

      if (Array.isArray(res)) {
        setCourses(res);
      } else if (res?.content) {
        setCourses(res.content);
      } else if (res?.elements) {
        setCourses(res.elements);
      } else {
        setCourses([]);
      }
    } catch (err: any) {
      console.warn('Error fetching lecturer courses:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchCourses();
  }, [filterStatus]);

  const handleCreateCourse = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) return;

    setIsCreating(true);
    try {
      await ensureInstructorToken();
      const created = await instructorCourseApi.createCourse({
        title: newTitle.trim(),
        categoryId: Number(newCategoryId)
      });
      showSuccess(`Đã tạo khóa học bản nháp: "${created.title}"`);
      setShowCreateModal(false);
      setNewTitle('');
      fetchCourses();
      // Navigate directly into Course Settings Page
      onEditCourse(created.id);
    } catch (err: any) {
      showError(err.message || 'Lỗi khi tạo khóa học.');
    } finally {
      setIsCreating(false);
    }
  };

  const handleUnpublish = async () => {
    if (!unpublishCourseId) return;
    setIsUnpublishing(true);
    try {
      await instructorCourseApi.unpublish(unpublishCourseId, unpublishReason.trim() || undefined);
      showSuccess('Đã gỡ khóa học khỏi danh mục công khai.');
      setUnpublishCourseId(null);
      setUnpublishReason('');
      fetchCourses();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi gỡ khóa học.');
    } finally {
      setIsUnpublishing(false);
    }
  };

  const handleDeleteCourse = async () => {
    if (!deleteCourseId) return;
    try {
      await instructorCourseApi.deleteCourse(deleteCourseId);
      showSuccess('Đã xóa khóa học thành công.');
      setDeleteCourseId(null);
      fetchCourses();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi xóa khóa học.');
    }
  };

  const defaultThumbnail =
    'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=600&auto=format&fit=crop&q=60';

  return (
    <div className="flex-1 flex flex-col bg-background min-h-0 overflow-y-auto">
      {/* Studio Header Bar */}
      <header className="bg-surface-container-lowest border-b border-outline-variant/70 px-6 sm:px-8 py-5 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 shrink-0">
        <div>
          <h1 className="text-xl sm:text-2xl font-black text-slate-900 font-display m-0">
            Quản lý Khóa học Giảng viên
          </h1>
        
        </div>

        <button
          type="button"
          onClick={() => setShowCreateModal(true)}
          className="bg-primary hover:bg-primary/90 text-white font-extrabold text-xs px-5 py-2.5 rounded-full transition-all shadow-md shadow-primary/20 flex items-center gap-2 cursor-pointer active:scale-95 shrink-0"
        >
          <span className="material-symbols-outlined text-[18px]">add</span>
          <span>Tạo khóa học mới</span>
        </button>
      </header>

      {/* Main Content Body */}
      <div className="p-6 sm:p-8 space-y-6 max-w-7xl mx-auto w-full">
        {/* Filters & Search Toolbar */}
        <div className="bg-surface-container-lowest p-4 rounded-2xl border border-outline-variant/70 shadow-xs flex flex-col sm:flex-row items-center justify-between gap-4">
          {/* Status Filter Tabs */}
          <div className="flex items-center gap-1.5 overflow-x-auto w-full sm:w-auto">
            <button
              onClick={() => setFilterStatus('')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-colors cursor-pointer ${
                filterStatus === ''
                  ? 'bg-primary text-white'
                  : 'text-on-surface-variant hover:bg-surface-container-low'
              }`}
            >
              Tất cả
            </button>
            <button
              onClick={() => setFilterStatus('PUBLISHED')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-colors cursor-pointer ${
                filterStatus === 'PUBLISHED'
                  ? 'bg-emerald-600 text-white'
                  : 'text-on-surface-variant hover:bg-surface-container-low'
              }`}
            >
              Đã xuất bản
            </button>
            <button
              onClick={() => setFilterStatus('DRAFT')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-colors cursor-pointer ${
                filterStatus === 'DRAFT'
                  ? 'bg-amber-600 text-white'
                  : 'text-on-surface-variant hover:bg-surface-container-low'
              }`}
            >
              Bản nháp
            </button>
            <button
              onClick={() => setFilterStatus('UNPUBLISHED')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-colors cursor-pointer ${
                filterStatus === 'UNPUBLISHED'
                  ? 'bg-rose-600 text-white'
                  : 'text-on-surface-variant hover:bg-surface-container-low'
              }`}
            >
              Đã gỡ
            </button>
          </div>

          {/* Search form */}
          <form
            onSubmit={(e) => {
              e.preventDefault();
              fetchCourses();
            }}
            className="relative w-full sm:w-72"
          >
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline text-[18px]">
              search
            </span>
            <input
              type="text"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              placeholder="Tìm theo tiêu đề khóa học..."
              className="w-full pl-9 pr-3 py-1.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none"
            />
          </form>
        </div>

        {/* Courses Grid / Table */}
        {isLoading ? (
          <div className="text-center py-20 bg-surface-container-lowest rounded-3xl border border-outline-variant/60">
            <span className="inline-block animate-spin w-8 h-8 border-3 border-primary border-t-transparent rounded-full" />
            <p className="text-xs text-slate-500 font-bold mt-3">Đang tải danh sách khóa học...</p>
          </div>
        ) : courses.length === 0 ? (
          <div className="text-center py-16 bg-surface-container-lowest rounded-3xl border-2 border-dashed border-outline-variant/80 p-8 max-w-lg mx-auto space-y-4">
            <span className="material-symbols-outlined text-[56px] text-outline">school</span>
            <h3 className="text-base font-bold text-slate-900 m-0 font-display">
              Chưa có khóa học nào
            </h3>
            <p className="text-xs text-slate-500 m-0">
              Hãy bắt đầu xây dựng khóa học đầu tiên của bạn để chia sẻ kiến thức với học viên.
            </p>
            <button
              type="button"
              onClick={() => setShowCreateModal(true)}
              className="bg-primary hover:bg-primary/90 text-white text-xs font-bold px-5 py-2.5 rounded-full transition-all shadow-md shadow-primary/20 cursor-pointer"
            >
              Tạo khóa học ngay
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {courses.map((course) => (
              <div
                key={course.id}
                className="bg-surface-container-lowest border border-outline-variant/70 rounded-2xl overflow-hidden hover:shadow-lg transition-all duration-300 flex flex-col group"
              >
                {/* Thumbnail Header */}
                <div className="relative h-44 w-full bg-surface-container-low overflow-hidden">
                  <img
                    src={course.thumbnailUrl || defaultThumbnail}
                    alt={course.title}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    onError={(e) => {
                      (e.target as HTMLImageElement).src = defaultThumbnail;
                    }}
                  />
                  <div className="absolute top-3 left-3 flex items-center gap-1.5">
                    <StatusBadge status={course.status} />
                  </div>

                  <div className="absolute top-3 right-3">
                    <LevelBadge level={course.level} />
                  </div>
                </div>

                {/* Card Body */}
                <div className="p-5 flex flex-col flex-1 space-y-3">
                  <div>
                    <span className="text-[10px] font-bold text-primary uppercase tracking-wider block mb-1">
                      {course.categoryName || course.category?.name || 'Chưa phân loại'}
                    </span>
                    <h3 className="font-bold text-sm sm:text-base text-slate-900 group-hover:text-primary transition-colors line-clamp-2 m-0 leading-snug font-display">
                      {course.title}
                    </h3>
                  </div>

                  <div className="flex items-center justify-between text-xs text-slate-500 pt-2 border-t border-slate-100">
                    <span className="font-extrabold text-sm text-primary font-display">
                      {formatCurrencyVND(course.price)}
                    </span>
                    <span className="text-[11px] font-mono">
                      Cập nhật: {formatDate(course.updatedAt || course.createdAt)}
                    </span>
                  </div>

                  {/* Action Buttons */}
                  <div className="pt-2 border-t border-slate-100 flex items-center justify-between gap-2 mt-auto">
                    <div className="flex items-center gap-1.5 flex-1">
                      <button
                        type="button"
                        onClick={() => onEditCourse(course.id)}
                        className="flex-1 bg-surface-container-low hover:bg-surface-container text-on-surface font-bold text-xs py-2 px-2.5 rounded-xl transition-colors flex items-center justify-center gap-1 cursor-pointer border border-outline-variant/60"
                        title="Cài đặt thông tin và giá bán (US-05)"
                      >
                        <span className="material-symbols-outlined text-[16px]">tune</span>
                        <span>Cài đặt & Giá</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => onEditCurriculum(course.id)}
                        className="flex-1 bg-primary/10 hover:bg-primary text-primary hover:text-white font-bold text-xs py-2 px-2.5 rounded-xl transition-all flex items-center justify-center gap-1 cursor-pointer"
                        title="Biên soạn bài giảng đa định dạng (US-06)"
                      >
                        <span className="material-symbols-outlined text-[16px]">menu_book</span>
                        <span>Soạn bài</span>
                      </button>
                    </div>

                    {/* Quick Menu Actions */}
                    <div className="flex items-center gap-1 shrink-0">
                      {course.status !== 'PUBLISHED' ? (
                        <button
                          type="button"
                          onClick={() => setPublishCheckCourseId(course.id)}
                          className="p-2 text-emerald-600 hover:bg-emerald-50 rounded-xl transition-colors cursor-pointer"
                          title="Kiểm tra & Xuất bản khóa học"
                        >
                          <span className="material-symbols-outlined text-[18px]">publish</span>
                        </button>
                      ) : (
                        <button
                          type="button"
                          onClick={() => setUnpublishCourseId(course.id)}
                          className="p-2 text-amber-600 hover:bg-amber-50 rounded-xl transition-colors cursor-pointer"
                          title="Gỡ khóa học khỏi danh mục (Unpublish)"
                        >
                          <span className="material-symbols-outlined text-[18px]">visibility_off</span>
                        </button>
                      )}

                      <button
                        type="button"
                        onClick={() => setStatusLogsCourseId(course.id)}
                        className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer"
                        title="Xem lịch sử trạng thái"
                      >
                        <span className="material-symbols-outlined text-[18px]">history</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => setDeleteCourseId(course.id)}
                        className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition-colors cursor-pointer"
                        title="Xóa khóa học"
                      >
                        <span className="material-symbols-outlined text-[18px]">delete</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* CREATE COURSE MODAL */}
      {showCreateModal && (
        <Modal
          isOpen={showCreateModal}
          onClose={() => setShowCreateModal(false)}
          title="Tạo khóa học mới (DRAFT)"
          subtitle="Khởi tạo bản nháp khóa học để bắt đầu thiết lập thông tin và giáo trình."
          maxWidth="md"
          icon="add_circle"
        >
          <form onSubmit={handleCreateCourse} className="space-y-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                Tiêu đề khóa học
              </label>
              <input
                type="text"
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
                placeholder="Nhập tiêu đề khóa học lôi cuốn (vd: Lập trình Fullstack với Spring Boot & React)..."
                className="w-full px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none"
                autoFocus
                required
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                Danh mục khóa học
              </label>
              <select
                value={newCategoryId}
                onChange={(e) => setNewCategoryId(Number(e.target.value))}
                className="w-full px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface font-bold focus:bg-white focus:border-primary focus:outline-none"
              >
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="submit"
                disabled={isCreating}
                className="px-5 py-2 rounded-xl text-xs font-bold text-white bg-primary hover:bg-primary/90 shadow-md shadow-primary/20 flex items-center gap-1.5 cursor-pointer"
              >
                {isCreating && (
                  <span className="inline-block animate-spin w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full" />
                )}
                <span>Tạo bản nháp DRAFT</span>
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* PUBLISH CHECK MODAL */}
      {publishCheckCourseId && (
        <PublishCheckModal
          isOpen={!!publishCheckCourseId}
          onClose={() => setPublishCheckCourseId(null)}
          courseId={publishCheckCourseId}
          onPublishSuccess={() => fetchCourses()}
        />
      )}

      {/* STATUS LOGS MODAL */}
      {statusLogsCourseId && (
        <StatusLogsModal
          isOpen={!!statusLogsCourseId}
          onClose={() => setStatusLogsCourseId(null)}
          courseId={statusLogsCourseId}
        />
      )}

      {/* UNPUBLISH MODAL */}
      {unpublishCourseId && (
        <Modal
          isOpen={!!unpublishCourseId}
          onClose={() => setUnpublishCourseId(null)}
          title="Gỡ khóa học khỏi Catalog (Unpublish)"
          subtitle="Khóa học sẽ chuyển về trạng thái UNPUBLISHED và không còn xuất hiện trên trang Học viên."
          maxWidth="md"
          icon="visibility_off"
        >
          <div className="space-y-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                Lý do gỡ khóa học (Tùy chọn)
              </label>
              <textarea
                rows={3}
                value={unpublishReason}
                onChange={(e) => setUnpublishReason(e.target.value)}
                placeholder="Nhập lý do gỡ (vd: Cập nhật lại nội dung chương 1, điều chỉnh giá bán)..."
                className="w-full px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setUnpublishCourseId(null)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleUnpublish}
                disabled={isUnpublishing}
                className="px-4 py-2 rounded-xl text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 shadow-sm cursor-pointer"
              >
                {isUnpublishing ? 'Đang xử lý...' : 'Xác nhận gỡ khóa học'}
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* DELETE DIALOG */}
      {deleteCourseId && (
        <ConfirmDialog
          isOpen={!!deleteCourseId}
          onClose={() => setDeleteCourseId(null)}
          onConfirm={handleDeleteCourse}
          title="Xóa khóa học"
          message="Bạn có chắc chắn muốn xóa toàn bộ khóa học này không? Hành động này sẽ xóa các chương và bài giảng liên quan."
          confirmText="Xóa khóa học"
          isDestructive
        />
      )}
    </div>
  );
};
