import React, { useEffect, useState } from 'react';
import type { Curriculum, LearningOutcome } from '../../../types/course';
import { quizApi } from '../api/quizApi';

interface AlignmentMatrixProps {
  courseId: number;
  outcomes: LearningOutcome[];
  curriculum: Curriculum;
}

export const AlignmentMatrix: React.FC<AlignmentMatrixProps> = ({
  courseId,
  outcomes,
  curriculum
}) => {
  const [quizQuestionCounts, setQuizQuestionCounts] = useState<Record<number, number>>({});

  useEffect(() => {
    let cancelled = false;
    const assessments = curriculum.sections.flatMap((section) => section.assessments || []);
    Promise.all(
      assessments.map(async (assessment) => {
        try {
          const quiz = await quizApi.getQuiz(courseId, assessment.id);
          return [assessment.id, quiz.questions.length] as const;
        } catch {
          return [assessment.id, 0] as const;
        }
      })
    ).then((rows) => {
      if (!cancelled) setQuizQuestionCounts(Object.fromEntries(rows));
    });
    return () => {
      cancelled = true;
    };
  }, [courseId, curriculum]);

  const lessons = curriculum.sections.flatMap((section) => section.lessons || []);
  const assessments = curriculum.sections.flatMap((section) => section.assessments || []);
  const getLessonCount = (id: number) =>
    lessons.filter((lesson) => lesson.outcomeIds?.includes(id)).length;

  return (
    <section className="bg-surface-container-lowest p-6 sm:p-8 rounded-3xl border border-outline-variant/70 shadow-xs space-y-4">
      <div>
        <h2 className="text-base sm:text-lg font-extrabold text-slate-900 m-0 font-display flex items-center gap-2">
          <span className="material-symbols-outlined text-primary text-[24px]">grid_on</span>
          <span>Ma trận liên kết chuẩn đầu ra (Alignment Matrix)</span>
        </h2>
        <p className="text-xs text-slate-500 mt-1 m-0">
          Kiểm tra độ bao phủ: mỗi chuẩn đầu ra cần được giảng dạy qua bài học và kiểm tra qua quiz.
        </p>
      </div>

      <div className="overflow-x-auto rounded-2xl border border-outline-variant/60">
        <table className="w-full text-xs">
          <thead>
            <tr className="border-b border-outline-variant/60 bg-surface-container-low/60 text-left text-slate-600 font-bold">
              <th className="py-3 px-4">Chuẩn đầu ra</th>
              <th className="py-3 px-4 text-center">Bài học liên kết</th>
              <th className="py-3 px-4 text-center">Bài kiểm tra</th>
              <th className="py-3 px-4 text-right">Trạng thái</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline-variant/40 bg-white">
            {outcomes.length === 0 ? (
              <tr>
                <td colSpan={4} className="py-6 text-center text-slate-400">
                  Chưa có chuẩn đầu ra nào được thiết lập.
                </td>
              </tr>
            ) : (
              outcomes.map((outcome) => {
                const lessonCount = getLessonCount(outcome.id);
                const coveringAssessments = assessments.filter((assessment) =>
                  assessment.outcomeIds?.includes(outcome.id)
                );
                const quizCount = coveringAssessments.length;
                const allQuizzesHaveQuestions = coveringAssessments.every(
                  (assessment) => (quizQuestionCounts[assessment.id] || 0) > 0
                );
                const aligned = lessonCount > 0 && quizCount > 0 && allQuizzesHaveQuestions;

                return (
                  <tr key={outcome.id} className="hover:bg-surface-container-low/30 transition-colors">
                    <td className="py-3 px-4 font-semibold text-slate-800 min-w-[260px]">
                      {outcome.statement}
                    </td>
                    <td className="py-3 px-4 text-center font-bold text-slate-700">
                      <span className="px-2 py-0.5 rounded-lg bg-surface-container text-slate-700">
                        {lessonCount} bài
                      </span>
                    </td>
                    <td className="py-3 px-4 text-center font-bold text-slate-700">
                      <span className="px-2 py-0.5 rounded-lg bg-surface-container text-slate-700">
                        {quizCount} quiz
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right">
                      <span
                        className={`inline-flex items-center gap-1 text-[11px] font-bold px-2.5 py-0.5 rounded-full border ${
                          aligned
                            ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                            : 'bg-amber-50 text-amber-700 border-amber-200'
                        }`}
                      >
                        <span className="material-symbols-outlined text-[14px]">
                          {aligned ? 'check_circle' : 'pending'}
                        </span>
                        <span>{aligned ? 'Đủ liên kết' : 'Cần bổ sung'}</span>
                      </span>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
};
