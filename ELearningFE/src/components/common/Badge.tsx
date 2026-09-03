import React from 'react';
import type { CourseLevel, CourseStatus } from '../../types/course';

interface StatusBadgeProps {
  status: CourseStatus | string;
  size?: 'sm' | 'md';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'sm' }) => {
  let label = status;
  let bg = 'bg-slate-100 text-slate-700 border-slate-200';
  let dot = 'bg-slate-400';

  switch (status) {
    case 'PUBLISHED':
      label = 'Đã xuất bản';
      bg = 'bg-emerald-50 text-emerald-700 border-emerald-200';
      dot = 'bg-emerald-500';
      break;
    case 'DRAFT':
      label = 'Bản nháp';
      bg = 'bg-amber-50 text-amber-700 border-amber-200';
      dot = 'bg-amber-500';
      break;
    case 'UNPUBLISHED':
      label = 'Đã gỡ';
      bg = 'bg-rose-50 text-rose-700 border-rose-200';
      dot = 'bg-rose-500';
      break;
    case 'SUSPENDED':
      label = 'Đã khóa';
      bg = 'bg-red-50 text-red-800 border-red-200';
      dot = 'bg-red-600';
      break;
  }

  const padding = size === 'sm' ? 'px-2.5 py-0.5 text-[11px]' : 'px-3 py-1 text-xs';

  return (
    <span className={`inline-flex items-center gap-1.5 font-bold uppercase tracking-wider rounded-md border ${padding} ${bg}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${dot}`}></span>
      <span>{label}</span>
    </span>
  );
};

interface LevelBadgeProps {
  level: CourseLevel | string;
}

export const LevelBadge: React.FC<LevelBadgeProps> = ({ level }) => {
  switch (level) {
    case 'BEGINNER':
      return (
        <span className="bg-emerald-50 text-emerald-700 border border-emerald-200 text-[10px] font-bold px-2 py-0.5 rounded-md uppercase">
          Cơ bản
        </span>
      );
    case 'INTERMEDIATE':
      return (
        <span className="bg-blue-50 text-blue-700 border border-blue-200 text-[10px] font-bold px-2 py-0.5 rounded-md uppercase">
          Trung cấp
        </span>
      );
    case 'ADVANCED':
      return (
        <span className="bg-purple-50 text-purple-700 border border-purple-200 text-[10px] font-bold px-2 py-0.5 rounded-md uppercase">
          Nâng cao
        </span>
      );
    default:
      return (
        <span className="bg-slate-100 text-slate-700 border border-slate-200 text-[10px] font-bold px-2 py-0.5 rounded-md uppercase">
          Mọi cấp độ
        </span>
      );
  }
};
