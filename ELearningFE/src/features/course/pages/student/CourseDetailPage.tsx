import React, { useState } from 'react';
import type { CourseDetail, Curriculum } from '../../../../types/course';
import { LevelBadge } from '../../../../components/common/Badge';
import { formatCurrencyVND } from '../../../../lib/formatters';

interface CourseDetailPageProps {
  course: CourseDetail | null;
  curriculum: Curriculum | null;
  isEnrolled: boolean;
  isLoading: boolean;
  isEnrolling: boolean;
  onEnroll: (courseId: number) => void;
  onGoToLearning: (courseId: number) => void;
  onBack: () => void;
}

export const CourseDetailPage: React.FC<CourseDetailPageProps> = ({
  course,
  curriculum,
  isEnrolled,
  isLoading,
  isEnrolling,
  onEnroll,
  onGoToLearning,
  onBack
}) => {
  const [showEnrollConfirm, setShowEnrollConfirm] = useState(false);

  if (isLoading || !course) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center p-8 flex-1">
        <div className="text-center">
          <span className="inline-block animate-spin border-4 border-primary border-t-transparent w-10 h-10 rounded-full" />
          <p className="text-on-surface-variant font-bold text-xs mt-4">
            Đang tải thông tin chi tiết khóa học...
          </p>
        </div>
      </div>
    );
  }

  const defaultThumbnail =
    'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=600&auto=format&fit=crop&q=60';

  return (
    <div className="min-h-screen bg-background pb-20 flex-1">
      {/* Top Banner Header */}
      <div className="bg-slate-900 text-white py-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto">
          <button
            onClick={onBack}
            className="inline-flex items-center gap-2 text-slate-300 hover:text-white text-xs font-bold mb-6 transition-colors cursor-pointer"
          >
            <span className="material-symbols-outlined text-[18px]">arrow_back</span>
            <span>Quay lại danh mục</span>
          </button>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
            
            {/* Left Course Summary info */}
            <div className="lg:col-span-2 space-y-4">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="bg-primary/40 text-indigo-200 border border-primary/40 text-[10px] font-bold px-2.5 py-1 rounded-md uppercase tracking-wider">
                  {course.categoryName || course.category?.name || 'Khóa học'}
                </span>
                <LevelBadge level={course.level} />
              </div>

              <h1 className="text-2xl sm:text-4xl font-extrabold text-white leading-tight font-display">
                {course.title}
              </h1>

              <p className="text-slate-300 text-sm sm:text-base leading-relaxed">
                {course.subtitle || course.description}
              </p>

              <div className="flex items-center gap-4 text-xs text-slate-400 pt-3 border-t border-slate-800 flex-wrap">
                <div className="flex items-center gap-1.5">
                  <span className="material-symbols-outlined text-[18px] text-primary-container">
                    person
                  </span>
                  <span>
                    Giảng viên: <strong className="text-slate-200">{course.lecturerName || 'Learnova Expert'}</strong>
                  </span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="material-symbols-outlined text-[18px] text-emerald-400">
                    verified
                  </span>
                  <span>Chất lượng kiểm duyệt</span>
                </div>
                {course.ratingAvg && (
                  <div className="flex items-center gap-1 text-amber-400 font-bold">
                    <span className="material-symbols-outlined text-[16px]">star</span>
                    <span>{course.ratingAvg}</span>
                  </div>
                )}
              </div>
            </div>

            {/* Right Card: Quick Enroll & Price */}
            <div className="bg-surface-container-lowest text-slate-900 rounded-3xl p-6 shadow-2xl border border-slate-200 space-y-5">
              <div className="relative h-48 w-full rounded-2xl overflow-hidden bg-slate-100">
                <img
                  src={course.thumbnailUrl || defaultThumbnail}
                  alt={course.title}
                  className="w-full h-full object-cover"
                />
              </div>

              <div className="flex items-baseline justify-between">
                <span className="text-xs font-bold text-slate-500 uppercase">Học phí</span>
                <span className="text-2xl font-black text-primary font-display">
                  {formatCurrencyVND(course.price)}
                </span>
              </div>

              {isEnrolled ? (
                <button
                  onClick={() => onGoToLearning(course.id)}
                  className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold py-3.5 px-6 rounded-2xl transition-all shadow-lg shadow-emerald-200 flex items-center justify-center gap-2 cursor-pointer active:scale-95"
                >
                  <span className="material-symbols-outlined text-[22px]">play_circle</span>
                  <span>VÀO HỌC NGAY</span>
                </button>
              ) : (
                <button
                  onClick={() => setShowEnrollConfirm(true)}
                  disabled={isEnrolling}
                  className="w-full bg-primary hover:bg-primary/90 text-white font-extrabold py-3.5 px-6 rounded-2xl transition-all shadow-lg shadow-primary/20 flex items-center justify-center gap-2 cursor-pointer active:scale-95"
                >
                  <span className="material-symbols-outlined text-[22px]">add_task</span>
                  <span>{isEnrolling ? 'Đang xử lý...' : 'ĐĂNG KÝ HỌC NGAY'}</span>
                </button>
              )}

              <div className="space-y-2 text-xs text-slate-500 pt-2 border-t border-slate-100">
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-[16px] text-primary">all_inclusive</span>
                  <span>Quyền truy cập trọn đời</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-[16px] text-primary">devices</span>
                  <span>Học trên máy tính và thiết bị di động</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-[16px] text-primary">workspace_premium</span>
                  <span>Chứng nhận hoàn thành khóa học</span>
                </div>
              </div>
            </div>

          </div>

        </div>
      </div>

      {/* Main Body Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          <div className="lg:col-span-2 space-y-8">
            
            {/* 3 Bullets Highlights */}
            {course.learningObjectives && course.learningObjectives.length > 0 && (
              <div className="bg-surface-container-lowest rounded-3xl p-6 shadow-sm border border-outline-variant/60 space-y-4">
                <h3 className="text-base font-extrabold text-slate-900 m-0 flex items-center gap-2 font-display">
                  <span className="material-symbols-outlined text-emerald-600">check_circle</span>
                  Bạn sẽ học được gì
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs text-slate-700">
                  {course.learningObjectives.map((obj, i) => (
                    <div key={i} className="flex items-start gap-2.5">
                      <span className="material-symbols-outlined text-emerald-600 text-[16px] shrink-0 mt-0.5">
                        done
                      </span>
                      <span>{obj}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Description */}
            <div className="bg-surface-container-lowest rounded-3xl p-6 shadow-sm border border-outline-variant/60 space-y-4">
              <h3 className="text-base font-extrabold text-slate-900 m-0 flex items-center gap-2 font-display">
                <span className="material-symbols-outlined text-primary">info</span>
                Giới thiệu khóa học
              </h3>
              <div className="text-xs sm:text-sm text-slate-700 leading-relaxed whitespace-pre-line">
                {course.description || 'Chưa có thông tin mô tả chi tiết.'}
              </div>
            </div>

            {/* Requirements & Target Audiences */}
            {(course.requirements?.length || course.targetAudiences?.length) ? (
              <div className="bg-surface-container-lowest rounded-3xl p-6 shadow-sm border border-outline-variant/60 space-y-6">
                {course.requirements && course.requirements.length > 0 && (
                  <div className="space-y-3">
                    <h4 className="text-xs font-bold uppercase tracking-wider text-slate-900 m-0">
                      Yêu cầu đầu vào
                    </h4>
                    <ul className="list-disc list-inside space-y-1 text-xs text-slate-600 m-0 pl-1">
                      {course.requirements.map((req, i) => (
                        <li key={i}>{req}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {course.targetAudiences && course.targetAudiences.length > 0 && (
                  <div className="space-y-3 pt-4 border-t border-slate-100">
                    <h4 className="text-xs font-bold uppercase tracking-wider text-slate-900 m-0">
                      Khóa học này dành cho ai
                    </h4>
                    <ul className="list-disc list-inside space-y-1 text-xs text-slate-600 m-0 pl-1">
                      {course.targetAudiences.map((aud, i) => (
                        <li key={i}>{aud}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            ) : null}

            {/* Curriculum Tree */}
            <div className="bg-surface-container-lowest rounded-3xl p-6 shadow-sm border border-outline-variant/60 space-y-4">
              <div className="flex justify-between items-center">
                <h3 className="text-base font-extrabold text-slate-900 m-0 flex items-center gap-2 font-display">
                  <span className="material-symbols-outlined text-primary">list_alt</span>
                  Giáo trình bài học
                </h3>
                <span className="text-xs font-bold text-slate-500 bg-slate-100 px-3 py-1 rounded-full">
                  {curriculum?.sections.length || 0} chương
                </span>
              </div>

              {!curriculum || curriculum.sections.length === 0 ? (
                <p className="text-xs text-slate-400 italic py-4">Giáo trình đang được cập nhật...</p>
              ) : (
                <div className="space-y-3">
                  {curriculum.sections.map((sec, sIdx) => (
                    <div key={sec.id} className="border border-outline-variant/70 rounded-2xl overflow-hidden">
                      <div className="bg-surface-container-low px-5 py-3.5 border-b border-outline-variant/60 flex justify-between items-center">
                        <span className="font-extrabold text-xs text-slate-800">
                          Chương {sIdx + 1}: {sec.title}
                        </span>
                        <span className="text-[11px] font-semibold text-slate-500">
                          {sec.lessons.length} bài học
                        </span>
                      </div>

                      <div className="divide-y divide-slate-100">
                        {sec.lessons.map((les, lIdx) => (
                          <div
                            key={les.id}
                            className="px-5 py-3 flex items-center justify-between text-xs hover:bg-slate-50/70"
                          >
                            <div className="flex items-center gap-3">
                              <span className="material-symbols-outlined text-slate-400 text-[18px]">
                                {les.contentType === 'VIDEO'
                                  ? 'play_circle'
                                  : les.contentType === 'ARTICLE'
                                  ? 'article'
                                  : 'description'}
                              </span>
                              <span className="font-medium text-slate-800">
                                Bài {lIdx + 1}: {les.title}
                              </span>
                            </div>
                            <div className="flex items-center gap-2">
                              {les.isPreview && (
                                <span className="bg-primary/10 text-primary font-bold text-[10px] px-2 py-0.5 rounded border border-primary/20">
                                  Học thử
                                </span>
                              )}
                              <span className="text-slate-400 text-[10px]">
                                {les.contentType === 'VIDEO' ? 'Video' : 'Tài liệu'}
                              </span>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

          </div>

        </div>
      </div>

      {/* Confirmation Dialog */}
      {showEnrollConfirm && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl space-y-5 border border-slate-200">
            <div className="text-center space-y-2">
              <div className="w-12 h-12 bg-primary/10 text-primary rounded-2xl flex items-center justify-center mx-auto">
                <span className="material-symbols-outlined text-[28px]">school</span>
              </div>
              <h3 className="text-lg font-black text-slate-900 m-0 font-display">
                Xác nhận ghi danh khóa học
              </h3>
              <p className="text-xs text-slate-500 m-0">
                Bạn chuẩn bị đăng ký học khóa <strong className="text-slate-900">{course.title}</strong>.
              </p>
            </div>

            <div className="bg-surface-container-low p-4 rounded-2xl text-xs space-y-1.5 text-slate-600 border border-outline-variant/60">
              <div className="flex justify-between">
                <span>Học phí:</span>
                <strong className="text-primary font-black font-display">
                  {formatCurrencyVND(course.price)}
                </strong>
              </div>
              <div className="flex justify-between">
                <span>Quyền truy cập:</span>
                <strong className="text-emerald-600 font-bold">Trọn đời</strong>
              </div>
            </div>

            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => setShowEnrollConfirm(false)}
                className="flex-1 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold py-2.5 rounded-xl text-xs transition-colors cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowEnrollConfirm(false);
                  onEnroll(course.id);
                }}
                disabled={isEnrolling}
                className="flex-1 bg-primary hover:bg-primary/90 text-white font-extrabold py-2.5 rounded-xl text-xs transition-all shadow-md shadow-primary/20 cursor-pointer"
              >
                Xác nhận Đăng ký
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
