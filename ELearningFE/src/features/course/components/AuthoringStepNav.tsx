import React from 'react';
import { useNavigate } from 'react-router-dom';

type AuthoringStep = 'info' | 'assessment';

interface AuthoringStepNavProps {
  courseId: number;
  active: AuthoringStep;
}

const steps: Array<{ id: AuthoringStep; label: string; anchor: string; route: 'settings' | 'curriculum' }> = [
  { id: 'info', label: 'Thông tin', anchor: '', route: 'settings' },
  { id: 'assessment', label: 'Soạn giáo trình', anchor: 'curriculum', route: 'curriculum' },
];

export const AuthoringStepNav: React.FC<AuthoringStepNavProps> = ({ courseId, active }) => {
  const navigate = useNavigate();
  return (
    <nav className="flex flex-wrap items-center gap-1.5 rounded-2xl bg-slate-100 p-1.5" aria-label="Course authoring steps">
      {steps.map((step, index) => (
        <button key={step.id} type="button" onClick={() => navigate(`/instructor/courses/${courseId}/${step.route}${step.anchor ? `#${step.anchor}` : ''}`)} className={`rounded-xl px-3 py-2 text-[11px] font-black transition-colors cursor-pointer ${active === step.id ? 'bg-white text-primary shadow-sm' : 'text-slate-500 hover:bg-white/70'}`}>
          <span className="mr-1 text-slate-400">{index + 1}.</span>{step.label}
        </button>
      ))}
    </nav>
  );
};
