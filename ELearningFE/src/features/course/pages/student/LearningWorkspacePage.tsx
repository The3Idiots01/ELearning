import React, { useState, useEffect } from 'react';
import type { Assessment, CourseDetail, Curriculum, Lesson, ProgressSnapshot } from '../../../../types/course';
import { formatDuration } from '../../../../lib/formatters';
import { DocumentViewer } from '../../components/DocumentViewer';
import { LessonVideoPlayer } from '../../components/LessonVideoPlayer';
import { playbackApi } from '../../api/playbackApi';
import { ApiError } from '../../../../lib/apiClient';
import { useCourseProgress } from '../../hooks/useCourseProgress';
import { QuizTakingView } from '../../components/QuizTakingView';
import { LessonQaSection } from '../../components/LessonQaSection';
import { CourseReviewModal } from '../../components/reviews/CourseReviewModal';
import { WorkspaceReviewsTab } from '../../components/reviews/WorkspaceReviewsTab';
import { AssessmentReviewPromptModal } from '../../components/reviews/AssessmentReviewPromptModal';
import { reviewApi } from '../../api/reviewApi';
import type { CourseReview, CourseReviewSummary } from '../../../../types/review';

interface LearningWorkspacePageProps {
  courseId: number;
  courseDetail: CourseDetail | null;
  curriculum: Curriculum | null;
  isLoading: boolean;
  courseProgressPercent?: number | null;
  onCompleteLesson: (lessonId: number) => Promise<boolean>;
  onLessonProgress: (snapshot: ProgressSnapshot) => void;
  onAssessmentCompleted?: () => void;
  onBack: () => void;
}

export const LearningWorkspacePage: React.FC<LearningWorkspacePageProps> = ({
  courseId,
  courseDetail,
  curriculum,
  isLoading,
  courseProgressPercent,
  onCompleteLesson,
  onLessonProgress,
  onAssessmentCompleted,
  onBack
}) => {
  const [activeLessonId, setActiveLessonId] = useState<number | null>(null);
  const activeLesson = curriculum?.sections.flatMap(s => s.lessons).find(l => l.id === activeLessonId) ?? null;
  const setActiveLesson = (lesson: Lesson | null) => setActiveLessonId(lesson?.id ?? null);
  const [activeAssessment, setActiveAssessment] = useState<Assessment | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'qa' | 'reviews'>('overview');
  const [isCompleting, setIsCompleting] = useState(false);
  const [expandedSections, setExpandedSections] = useState<Record<number, boolean>>({});
  const [playbackUrl, setPlaybackUrl] = useState<string | null>(null);
  const [playbackError, setPlaybackError] = useState<string | null>(null);
  const [isLoadingPlayback, setIsLoadingPlayback] = useState(false);
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  const [initialModalRating, setInitialModalRating] = useState<number | undefined>(undefined);
  const [isAssessmentReviewPromptOpen, setIsAssessmentReviewPromptOpen] = useState(false);
  const [myReview, setMyReview] = useState<CourseReview | null>(null);
  const [summary, setSummary] = useState<CourseReviewSummary | null>(null);

  useEffect(() => {
    reviewApi.getMyReview(courseId).then(setMyReview).catch(() => {});
    reviewApi.getCourseReviewSummary(courseId).then(setSummary).catch(() => {});
  }, [courseId]);

  const handleOpenReviewModal = (rating?: number) => {
    setInitialModalRating(rating);
    setIsReviewModalOpen(true);
  };

  const handleSelectRatingFromPrompt = (rating: number) => {
    setIsAssessmentReviewPromptOpen(false);
    handleOpenReviewModal(rating);
  };

  const handleDismissPrompt = () => {
    setIsAssessmentReviewPromptOpen(false);
    sessionStorage.setItem(`dismissed_review_prompt_${courseId}`, 'true');
  };

  const handleDeleteReview = async () => {
    try {
      await reviewApi.deleteMyReview(courseId);
      setMyReview(null);
      reviewApi.getCourseReviewSummary(courseId).then(setSummary).catch(() => {});
    } catch {
      // Ignored
    }
  };

  const handleAssessmentCompletedInternal = () => {
    onAssessmentCompleted?.();

    // Coursera-style milestone review prompt:
    const dismissedKey = `dismissed_review_prompt_${courseId}`;
    const wasDismissed = sessionStorage.getItem(dismissedKey);

    if (!myReview && overallProgress >= 20 && !wasDismissed) {
      setTimeout(() => {
        setIsAssessmentReviewPromptOpen(true);
      }, 700);
    }
  };

  // Xin PlaybackTicket khi bấm phát, thay vì dùng URL ký sẵn hàng loạt trong
  // curriculum (§7.1 G1). VIDEO dùng LessonVideoPlayer riêng (Task 10 — có
  // resume + heartbeat + watermark); ở đây chỉ còn lo FILE, và ARTICLE không
  // cần ticket vì chỉ có contentText.
  useEffect(() => {
    const needsPlayback = activeLesson?.contentType === 'FILE';
    if (!activeLesson || !needsPlayback || !activeLesson.playable) {
      setPlaybackUrl(null);
      setPlaybackError(null);
      return;
    }

    let cancelled = false;
    setPlaybackUrl(null);
    setPlaybackError(null);
    setIsLoadingPlayback(true);

    playbackApi
      .getPlaybackTicket(courseId, activeLesson.id)
      .then((ticket) => {
        if (!cancelled) setPlaybackUrl(ticket.streamUrl);
      })
      .catch((err) => {
        if (cancelled) return;
        const message =
          err instanceof ApiError
            ? err.message
            : 'Không thể tải video bài giảng. Vui lòng thử lại.';
        setPlaybackError(message);
      })
      .finally(() => {
        if (!cancelled) setIsLoadingPlayback(false);
      });

    return () => {
      cancelled = true;
    };
  }, [courseId, activeLesson]);

  useEffect(() => {
    if (curriculum && curriculum.sections.length > 0) {
      const initialExpand: Record<number, boolean> = {};
      curriculum.sections.forEach((sec) => {
        initialExpand[sec.id] = true;
      });
      setExpandedSections(initialExpand);

      if (!activeLesson && !activeAssessment) {
        for (const sec of curriculum.sections) {
          if (sec.lessons.length > 0) {
            setActiveLesson(sec.lessons[0]);
            break;
          }
        }
      }
    }
  }, [curriculum, activeAssessment, activeLesson]);

  const { totalUnits, completedUnits, percent: overallProgress } = useCourseProgress(
    curriculum,
    courseProgressPercent
  );

  if (isLoading || !curriculum) {
    return (
      <div className="min-h-screen bg-slate-950 text-white flex items-center justify-center p-8 flex-1">
        <div className="text-center">
          <span className="inline-block animate-spin border-4 border-primary border-t-transparent w-10 h-10 rounded-full" />
          <p className="text-slate-400 font-bold text-xs mt-4">
            Đang tải phòng học trực tuyến...
          </p>
        </div>
      </div>
    );
  }

  const toggleSection = (sectionId: number) => {
    setExpandedSections((prev) => ({ ...prev, [sectionId]: !prev[sectionId] }));
  };

  const findNextLesson = (): Lesson | null => {
    if (!activeLesson) return null;
    let foundCurrent = false;
    for (const sec of curriculum.sections) {
      for (const les of sec.lessons) {
        if (foundCurrent) return les;
        if (les.id === activeLesson.id) foundCurrent = true;
      }
    }
    return null;
  };

  const handleMarkComplete = async () => {
    if (!activeLesson || isCompleting) return;
    setIsCompleting(true);
    const success = await onCompleteLesson(activeLesson.id);
    setIsCompleting(false);
    if (success) {
      const next = findNextLesson();
      if (next) {
        setActiveLesson(next);
      }
    }
  };

  const nextLesson = findNextLesson();

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans flex-1">
      {/* Top Classroom Bar */}
      <header className="bg-slate-900 border-b border-slate-800 px-4 sm:px-6 py-3 flex items-center justify-between gap-4 z-20 shrink-0">
        <div className="flex items-center gap-4 overflow-hidden">
          <button
            onClick={onBack}
            className="p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-xl transition-colors cursor-pointer shrink-0"
            title="Quay lại chi tiết khóa học"
          >
            <span className="material-symbols-outlined text-[20px]">arrow_back</span>
          </button>

          <div className="overflow-hidden">
            <span className="text-[10px] font-bold text-indigo-400 uppercase tracking-wider block">
              Không gian Học tập Learnova
            </span>
            <h1 className="text-xs sm:text-sm font-extrabold text-white truncate m-0 font-display">
              {courseDetail?.title || 'Đang tham gia khóa học'}
            </h1>
          </div>
        </div>

        {/* Overall Progress Indicator & Review Button */}
        <div className="flex items-center gap-3 sm:gap-4 shrink-0">
          {myReview ? (
            <button
              onClick={() => setIsReviewModalOpen(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-amber-300 hover:text-amber-200 border border-slate-700 text-xs font-bold transition-all cursor-pointer shadow-sm"
              title="Chỉnh sửa đánh giá của bạn"
            >
              <span className="material-symbols-outlined text-[16px]" style={{ fontVariationSettings: "'FILL' 1" }}>
                star
              </span>
              <span className="hidden sm:inline">Đã đánh giá</span>
            </button>
          ) : overallProgress < 20 ? (
            <div className="relative group">
              <button
                type="button"
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800/80 text-slate-400 border border-slate-700/60 text-xs font-semibold cursor-not-allowed opacity-75"
              >
                <span className="material-symbols-outlined text-[16px] text-amber-500/80">
                  lock_clock
                </span>
                <span className="hidden sm:inline">Cần 20% để đánh giá</span>
              </button>
              <div className="absolute right-0 top-full mt-2 hidden group-hover:flex flex-col items-center z-50 w-56 p-2.5 rounded-xl bg-slate-900 text-white text-[11px] font-medium shadow-2xl border border-slate-800 pointer-events-none text-center leading-relaxed">
                <div className="w-2 h-2 bg-slate-900 border-l border-t border-slate-800 rotate-45 -mt-3.5 mb-1" />
                <span>Hoàn thành tối thiểu <strong>20%</strong> để mở tính năng đánh giá.</span>
                <span className="text-amber-400 font-bold mt-0.5">Hiện tại: {overallProgress}%</span>
              </div>
            </div>
          ) : (
            <button
              onClick={() => setIsReviewModalOpen(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-amber-300 hover:text-amber-200 border border-slate-700 text-xs font-bold transition-all cursor-pointer shadow-sm"
              title="Đánh giá khóa học"
            >
              <span className="material-symbols-outlined text-[16px]" style={{ fontVariationSettings: "'FILL' 1" }}>
                star
              </span>
              <span className="hidden sm:inline">Đánh giá</span>
            </button>
          )}

          <div className="hidden sm:flex flex-col items-end">
            <span className="text-[10px] font-bold text-slate-400 uppercase">Tiến độ</span>
            <span className="text-xs font-black text-emerald-400">{overallProgress}% Hoàn thành</span>
          </div>
          <div className="w-20 sm:w-32 bg-slate-800 rounded-full h-2 overflow-hidden border border-slate-700">
            <div
              className="bg-emerald-500 h-full rounded-full transition-all duration-500"
              style={{ width: `${overallProgress}%` }}
            />
          </div>
        </div>
      </header>

      {/* Main Classroom Body */}
      <div className="flex-1 flex flex-col lg:flex-row overflow-hidden min-h-0">
        
        {/* Left Primary Player / Content Area */}
        <main className="flex-1 flex flex-col bg-slate-950 overflow-y-auto min-h-0">
          
          {/* Media / Document Player Box */}
          {activeAssessment ? (
            <QuizTakingView
              courseId={courseId}
              assessmentId={activeAssessment.id}
              onSubmitted={handleAssessmentCompletedInternal}
            />
          ) : !activeLesson ? (
            <div className="w-full bg-black aspect-video max-h-[60vh] flex items-center justify-center relative border-b border-slate-800 shrink-0">
              <div className="text-center p-8 text-slate-400">
                <span className="material-symbols-outlined text-[48px] mb-2">touch_app</span>
                <p className="text-xs font-bold m-0">Vui lòng chọn bài học từ danh sách bên phải.</p>
              </div>
            </div>
          ) : activeLesson.contentType === 'VIDEO' ? (
            <div className="w-full bg-black aspect-video max-h-[60vh] flex items-center justify-center relative border-b border-slate-800 shrink-0">
              <LessonVideoPlayer key={courseId + ":" + activeLesson.id} courseId={courseId} lesson={activeLesson} onProgress={onLessonProgress} />
            </div>
          ) : activeLesson.contentType === 'ARTICLE' ? (
            /* ARTICLE Content Type */
            <div className="w-full bg-slate-950 p-6 sm:p-8 border-b border-slate-800 shrink-0">
              <div className="max-w-4xl mx-auto space-y-4">
                <div className="flex items-center gap-2 text-primary-container text-xs font-bold uppercase tracking-wider">
                  <span className="material-symbols-outlined text-[18px]">article</span>
                  <span>Bài viết lý thuyết</span>
                </div>
                <h2 className="text-xl sm:text-2xl font-bold text-white m-0 font-display">{activeLesson.title}</h2>
                <div className="text-xs sm:text-sm text-slate-300 leading-relaxed whitespace-pre-line bg-slate-900/60 p-6 sm:p-8 rounded-2xl border border-slate-800 shadow-inner">
                  {activeLesson.contentText || 'Bài học này bao gồm tài liệu nghiên cứu chi tiết.'}
                </div>
              </div>
            </div>
          ) : (
            /* FILE Content Type */
            <div className="w-full min-h-[1400px] sm:min-h-[1600px] p-3 sm:p-5 border-b border-slate-800 flex flex-col shrink-0">
              {playbackError ? (
                <p className="text-xs text-rose-400 font-bold">{playbackError}</p>
              ) : isLoadingPlayback ? (
                <div className="flex-1 flex items-center justify-center text-slate-400">
                  <span className="material-symbols-outlined text-[40px] animate-spin">progress_activity</span>
                </div>
              ) : (
                <DocumentViewer
                  url={playbackUrl ?? undefined}
                  fileName={activeLesson.originalFileName}
                  mimeType={activeLesson.mimeType}
                  title={activeLesson.title}
                />
              )}
            </div>
          )}

          {/* Lesson Details & Actions Bar */}
          {activeLesson && (
            <div className="p-6 sm:p-8 space-y-6 max-w-5xl mx-auto w-full">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-6">
                <div>
                  <div className="flex items-center gap-2 mb-1.5">
                    <span className="bg-primary/20 text-indigo-300 border border-primary/30 text-[10px] font-bold px-2.5 py-0.5 rounded-md uppercase">
                      {activeLesson.contentType}
                    </span>
                    {activeLesson.completed && (
                      <span className="bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-[10px] font-bold px-2.5 py-0.5 rounded-md uppercase flex items-center gap-1">
                        <span className="material-symbols-outlined text-[12px]">check_circle</span>
                        <span>Đã hoàn thành</span>
                      </span>
                    )}
                  </div>

                  <h2 className="text-lg sm:text-xl font-bold text-white m-0 font-display">
                    {activeLesson.title}
                  </h2>
                </div>

                {/* Mark Complete & Next Buttons */}
                <div className="flex items-center gap-3">
                  <button
                    onClick={handleMarkComplete}
                    disabled={isCompleting || activeLesson.completed}
                    className={`px-5 py-2.5 rounded-xl text-xs font-extrabold transition-all flex items-center gap-2 cursor-pointer active:scale-95 ${
                      activeLesson.completed
                        ? 'bg-emerald-600/20 text-emerald-400 border border-emerald-500/30 cursor-default'
                        : 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg shadow-emerald-950/30'
                    }`}
                  >
                    <span className="material-symbols-outlined text-[18px]">
                      {activeLesson.completed ? 'task_alt' : 'check'}
                    </span>
                    <span>
                      {activeLesson.completed
                        ? 'Đã hoàn thành'
                        : isCompleting
                        ? 'Đang lưu...'
                        : 'Đánh dấu hoàn thành'}
                    </span>
                  </button>

                  {nextLesson && (
                    <button
                      onClick={() => setActiveLesson(nextLesson)}
                      className="bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold px-4 py-2.5 rounded-xl border border-slate-700 transition-all flex items-center gap-1.5 cursor-pointer"
                    >
                      <span>Bài tiếp theo</span>
                      <span className="material-symbols-outlined text-[18px]">navigate_next</span>
                    </button>
                  )}
                </div>
              </div>

              {/* Tab Navigation */}
              <div className="flex items-center gap-6 border-b border-slate-800 text-xs font-bold">
                <button
                  onClick={() => setActiveTab('overview')}
                  className={`pb-3 flex items-center gap-2 border-b-2 transition-colors cursor-pointer ${
                    activeTab === 'overview'
                      ? 'border-primary text-primary-container font-extrabold'
                      : 'border-transparent text-slate-400 hover:text-slate-200'
                  }`}
                >
                  <span className="material-symbols-outlined text-[18px]">info</span>
                  <span>Tổng quan & Tài liệu</span>
                </button>

                <button
                  onClick={() => setActiveTab('qa')}
                  className={`pb-3 flex items-center gap-2 border-b-2 transition-colors cursor-pointer ${
                    activeTab === 'qa'
                      ? 'border-primary text-primary-container font-extrabold'
                      : 'border-transparent text-slate-400 hover:text-slate-200'
                  }`}
                >
                  <span className="material-symbols-outlined text-[18px]">forum</span>
                  <span>Hỏi đáp (Q&A)</span>
                </button>

                <button
                  onClick={() => setActiveTab('reviews')}
                  className={`pb-3 flex items-center gap-2 border-b-2 transition-colors cursor-pointer ${
                    activeTab === 'reviews'
                      ? 'border-primary text-primary-container font-extrabold'
                      : 'border-transparent text-slate-400 hover:text-slate-200'
                  }`}
                >
                  <span className="material-symbols-outlined text-[18px] text-amber-400" style={{ fontVariationSettings: "'FILL' 1" }}>
                    star
                  </span>
                  <span>Đánh giá {summary && summary.totalReviews > 0 ? `(${summary.ratingAvg.toFixed(1)} ★)` : ''}</span>
                </button>
              </div>

              {/* Tab Content */}
              {activeTab === 'overview' ? (
                <div className="space-y-6">
                  {/* Lesson Supplementary Resources */}
                  {activeLesson.resources && activeLesson.resources.length > 0 ? (
                    <div className="bg-slate-900 rounded-2xl p-5 border border-slate-800 space-y-3">
                      <h4 className="text-xs font-extrabold text-slate-300 uppercase tracking-wider m-0 flex items-center gap-2">
                        <span className="material-symbols-outlined text-primary-container text-[18px]">
                          attach_file
                        </span>
                        <span>Tài liệu đính kèm ({activeLesson.resources.length})</span>
                      </h4>
                      <div className="divide-y divide-slate-800">
                        {activeLesson.resources.map((res) => (
                          <div key={res.id} className="py-2.5 flex items-center justify-between text-xs">
                            <span className="text-slate-300 font-medium truncate max-w-sm">{res.title}</span>
                            {res.downloadUrl && (
                              <a
                                href={res.downloadUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="text-primary-container hover:underline font-bold flex items-center gap-1"
                              >
                                <span>Tải về</span>
                                <span className="material-symbols-outlined text-[14px]">download</span>
                              </a>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <div className="bg-slate-900/40 rounded-2xl p-6 border border-slate-800/80 text-center">
                      <p className="text-xs text-slate-400 m-0">
                        Bài học này không có tài liệu đính kèm. Bạn có thắc mắc về bài học? Hãy chuyển sang tab <strong className="text-slate-200">Hỏi đáp (Q&A)</strong> để thảo luận cùng giảng viên và các bạn học viên!
                      </p>
                    </div>
                  )}
                </div>
              ) : activeTab === 'qa' ? (
                <LessonQaSection
                  courseId={courseId}
                  lessonId={activeLesson.id}
                  lessonTitle={activeLesson.title}
                />
              ) : (
                <WorkspaceReviewsTab
                  courseId={courseId}
                  courseTitle={courseDetail?.title || 'Khóa học'}
                  currentProgress={overallProgress}
                  myReview={myReview}
                  summary={summary}
                  onOpenReviewModal={handleOpenReviewModal}
                  onDeleteReview={handleDeleteReview}
                />
              )}
            </div>
          )}

        </main>

        {/* Right Sidebar: Curriculum Navigation */}
        <aside className="w-full lg:w-96 bg-slate-900 border-l border-slate-800 flex flex-col shrink-0 min-h-0">
          <div className="p-4 border-b border-slate-800 bg-slate-900/90 backdrop-blur-md sticky top-0 z-10 flex items-center justify-between">
            <h3 className="font-extrabold text-xs sm:text-sm text-white m-0 flex items-center gap-2 font-display">
              <span className="material-symbols-outlined text-primary-container text-[20px]">
                list_alt
              </span>
              <span>Nội dung khóa học</span>
            </h3>
            <span className="text-[10px] font-bold text-slate-400 bg-slate-800 px-2.5 py-1 rounded-full border border-slate-700">
              {completedUnits}/{totalUnits} mục
            </span>
          </div>

          <div className="flex-1 overflow-y-auto p-4 space-y-3">
            {curriculum.sections.map((section, sIdx) => {
              const isExpanded = expandedSections[section.id] ?? true;
              return (
                <div key={section.id} className="border border-slate-800 rounded-2xl overflow-hidden bg-slate-950/50">
                  <button
                    onClick={() => toggleSection(section.id)}
                    className="w-full p-3.5 bg-slate-800/80 hover:bg-slate-800 flex items-center justify-between text-left transition-colors border-b border-slate-800 cursor-pointer"
                  >
                    <span className="font-extrabold text-xs text-slate-200 line-clamp-1">
                      Chương {sIdx + 1}: {section.title}
                    </span>
                    <span className="material-symbols-outlined text-slate-400 text-[18px]">
                      {isExpanded ? 'expand_less' : 'expand_more'}
                    </span>
                  </button>

                  {isExpanded && (
                    <div className="divide-y divide-slate-900">
                      {section.lessons.map((les, lIdx) => {
                        const isSelected = activeLesson?.id === les.id;
                        return (
                          <button
                            key={les.id}
                            onClick={() => {
                              setActiveAssessment(null);
                              setActiveLesson(les);
                            }}
                            className={`w-full p-3.5 flex items-center justify-between text-left text-xs transition-all cursor-pointer ${
                              isSelected
                                ? 'bg-primary/20 text-white font-bold border-l-4 border-primary-container'
                                : 'text-slate-300 hover:bg-slate-800/40 font-medium'
                            }`}
                          >
                            <div className="flex items-center gap-3 overflow-hidden">
                              <span className="material-symbols-outlined text-[18px] shrink-0 text-slate-400">
                                {les.contentType === 'VIDEO'
                                  ? 'play_circle'
                                  : les.contentType === 'ARTICLE'
                                  ? 'article'
                                  : 'description'}
                              </span>
                              <span className="truncate">
                                Bài {lIdx + 1}: {les.title}
                              </span>
                            </div>

                            {les.completed ? (
                              <span
                                className="material-symbols-outlined text-emerald-400 text-[18px] shrink-0"
                                title="Đã hoàn thành"
                              >
                                check_circle
                              </span>
                            ) : (
                              <span className="text-[10px] text-slate-500 font-bold shrink-0">
                                {les.contentType === 'VIDEO' ? formatDuration(les.durationSeconds) : 'Đọc'}
                              </span>
                            )}
                          </button>
                        );
                      })}
                      {(section.assessments || []).map((assessment) => (
                        <button
                          key={`assessment-${assessment.id}`}
                          onClick={() => {
                            setActiveLesson(null);
                            setActiveAssessment(assessment);
                          }}
                          className={`w-full p-3.5 flex items-center justify-between text-left text-xs border-t border-slate-800/80 cursor-pointer ${activeAssessment?.id === assessment.id ? 'bg-amber-500/10 text-white border-l-4 border-amber-400' : 'text-slate-300 hover:bg-slate-800/40'}`}
                        >
                          <div className="flex items-center gap-3 overflow-hidden">
                            <span className="material-symbols-outlined text-[18px] shrink-0 text-amber-400">
                              quiz
                            </span>
                            <span className="truncate">Bài kiểm tra: {assessment.title}</span>
                          </div>
                          {assessment.completed ? (
                            <span className="material-symbols-outlined text-emerald-400 text-[18px] shrink-0">
                              check_circle
                            </span>
                          ) : (
                            <span className="text-[10px] text-slate-500 font-bold shrink-0">Quiz</span>
                          )}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </aside>

      </div>

      {/* Review Modal for Workspace */}
      <CourseReviewModal
        isOpen={isReviewModalOpen}
        onClose={() => setIsReviewModalOpen(false)}
        courseId={courseId}
        courseTitle={courseDetail?.title || 'Khóa học'}
        initialRating={initialModalRating}
        existingReview={myReview}
        onReviewSubmitted={(updated) => {
          setMyReview(updated);
          reviewApi.getCourseReviewSummary(courseId).then(setSummary).catch(() => {});
        }}
        onReviewDeleted={() => {
          setMyReview(null);
          reviewApi.getCourseReviewSummary(courseId).then(setSummary).catch(() => {});
        }}
      />

      {/* Coursera-style Post-Assessment Review Prompt */}
      <AssessmentReviewPromptModal
        isOpen={isAssessmentReviewPromptOpen}
        onClose={handleDismissPrompt}
        onSelectRating={handleSelectRatingFromPrompt}
        courseTitle={courseDetail?.title || 'Khóa học'}
        currentProgress={overallProgress}
      />
    </div>
  );
};
