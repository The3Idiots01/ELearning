import React, { useState, useEffect } from 'react';
import type { CourseDetail, Curriculum, Lesson } from '../../../../types/course';
import { formatDuration } from '../../../../lib/formatters';
import { DocumentViewer } from '../../components/DocumentViewer';

interface LearningWorkspacePageProps {
  courseId: number;
  courseDetail: CourseDetail | null;
  curriculum: Curriculum | null;
  isLoading: boolean;
  onCompleteLesson: (lessonId: number) => Promise<boolean>;
  onBack: () => void;
}

export const LearningWorkspacePage: React.FC<LearningWorkspacePageProps> = ({
  courseDetail,
  curriculum,
  isLoading,
  onCompleteLesson,
  onBack
}) => {
  const [activeLesson, setActiveLesson] = useState<Lesson | null>(null);
  const [isCompleting, setIsCompleting] = useState(false);
  const [expandedSections, setExpandedSections] = useState<Record<number, boolean>>({});

  useEffect(() => {
    if (curriculum && curriculum.sections.length > 0) {
      const initialExpand: Record<number, boolean> = {};
      curriculum.sections.forEach((sec) => {
        initialExpand[sec.id] = true;
      });
      setExpandedSections(initialExpand);

      if (!activeLesson) {
        for (const sec of curriculum.sections) {
          if (sec.lessons.length > 0) {
            setActiveLesson(sec.lessons[0]);
            break;
          }
        }
      }
    }
  }, [curriculum]);

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

  let totalLessons = 0;
  let completedCount = 0;
  curriculum.sections.forEach((sec) => {
    sec.lessons.forEach((les) => {
      totalLessons++;
      if (les.completed) completedCount++;
    });
  });
  const overallProgress = totalLessons > 0 ? Math.round((completedCount / totalLessons) * 100) : 0;

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
            title="Quay lại danh sách khóa học"
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

        {/* Overall Progress Indicator */}
        <div className="flex items-center gap-4 shrink-0">
          <div className="hidden sm:flex flex-col items-end">
            <span className="text-[10px] font-bold text-slate-400 uppercase">Tiến độ</span>
            <span className="text-xs font-black text-emerald-400">{overallProgress}% Hoàn thành</span>
          </div>
          <div className="w-24 sm:w-32 bg-slate-800 rounded-full h-2 overflow-hidden border border-slate-700">
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
          {!activeLesson ? (
            <div className="w-full bg-black aspect-video max-h-[60vh] flex items-center justify-center relative border-b border-slate-800 shrink-0">
              <div className="text-center p-8 text-slate-400">
                <span className="material-symbols-outlined text-[48px] mb-2">touch_app</span>
                <p className="text-xs font-bold m-0">Vui lòng chọn bài học từ danh sách bên phải.</p>
              </div>
            </div>
          ) : activeLesson.contentType === 'VIDEO' ? (
            <div className="w-full bg-black aspect-video max-h-[60vh] flex items-center justify-center relative border-b border-slate-800 shrink-0">
              {activeLesson.contentUrl ? (
                <video
                  key={activeLesson.contentUrl}
                  src={activeLesson.contentUrl}
                  controls
                  autoPlay
                  className="w-full h-full max-h-[60vh] object-contain"
                />
              ) : (
                <div className="text-center p-8 text-slate-400 space-y-3">
                  <span className="material-symbols-outlined text-[54px] text-primary-container animate-bounce">
                    play_circle
                  </span>
                  <h3 className="text-sm font-bold text-white m-0 font-display">
                    Video bài giảng: {activeLesson.title}
                  </h3>
                  <p className="text-xs text-slate-400 max-w-md mx-auto m-0">
                    Video bài học đã sẵn sàng. Bạn có thể bấm nút &quot;Đánh dấu hoàn thành&quot; phía dưới để tiếp tục tiến trình.
                  </p>
                </div>
              )}
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
              <DocumentViewer
                url={activeLesson.contentUrl}
                fileName={activeLesson.originalFileName}
                mimeType={activeLesson.mimeType}
                title={activeLesson.title}
              />
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

              {/* Lesson Supplementary Resources */}
              {activeLesson.resources && activeLesson.resources.length > 0 && (
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
              {completedCount}/{totalLessons} bài
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
                            onClick={() => setActiveLesson(les)}
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
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </aside>

      </div>
    </div>
  );
};
