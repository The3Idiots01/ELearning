import React, { useEffect, useState } from 'react';
import type { Curriculum, LearningOutcome } from '../../../types/course';
import { quizApi } from '../api/quizApi';

interface AlignmentMatrixProps {
  courseId: number;
  outcomes: LearningOutcome[];
  curriculum: Curriculum;
}

export const AlignmentMatrix: React.FC<AlignmentMatrixProps> = ({ courseId, outcomes, curriculum }) => {
  const [quizQuestionCounts, setQuizQuestionCounts] = useState<Record<number, number>>({});
  useEffect(() => {
    let cancelled = false;
    const assessments = curriculum.sections.flatMap((section) => section.assessments || []);
    Promise.all(assessments.map(async (assessment) => {
      try {
        const quiz = await quizApi.getQuiz(courseId, assessment.id);
        return [assessment.id, quiz.questions.length] as const;
      } catch {
        return [assessment.id, 0] as const;
      }
    })).then((rows) => {
      if (!cancelled) setQuizQuestionCounts(Object.fromEntries(rows));
    });
    return () => { cancelled = true; };
  }, [courseId, curriculum]);

  const lessons = curriculum.sections.flatMap((section) => section.lessons || []);
  const assessments = curriculum.sections.flatMap((section) => section.assessments || []);
  const getLessonCount = (id: number) => lessons.filter((lesson) => lesson.outcomeIds?.includes(id)).length;

  return (
    <section className="bg-surface-container-lowest p-6 rounded-3xl border border-indigo-200 shadow-xs space-y-4">
      <div><h2 className="text-base font-black text-slate-900 m-0 flex items-center gap-2"><span className="material-symbols-outlined text-indigo-500">grid_on</span>Alignment matrix</h2><p className="text-xs text-slate-500 mt-1 m-0">Kiểm tra nhanh mỗi outcome đã có lesson và quiz cover hay chưa.</p></div>
      <div className="overflow-x-auto"><table className="w-full text-xs"><thead><tr className="border-b border-slate-200 text-left text-slate-500"><th className="py-2 pr-4">Learning outcome</th><th className="py-2 px-4">Lessons</th><th className="py-2 px-4">Quizzes</th><th className="py-2 pl-4">Trạng thái</th></tr></thead><tbody>{outcomes.map((outcome) => { const lessonCount = getLessonCount(outcome.id); const coveringAssessments = assessments.filter((assessment) => assessment.outcomeIds?.includes(outcome.id)); const quizCount = coveringAssessments.length; const allQuizzesHaveQuestions = coveringAssessments.every((assessment) => (quizQuestionCounts[assessment.id] || 0) > 0); const aligned = lessonCount > 0 && quizCount > 0 && allQuizzesHaveQuestions; return <tr key={outcome.id} className="border-b border-slate-100"><td className="py-3 pr-4 font-semibold text-slate-800 min-w-[260px]">{outcome.statement}</td><td className="py-3 px-4">{lessonCount}</td><td className="py-3 px-4">{quizCount}</td><td className={`py-3 pl-4 font-black ${aligned ? 'text-emerald-600' : 'text-amber-600'}`}>{aligned ? 'Đủ liên kết' : 'Cần bổ sung'}</td></tr>; })}</tbody></table></div>
    </section>
  );
};
