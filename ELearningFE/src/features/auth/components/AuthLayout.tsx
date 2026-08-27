import React from 'react';

interface AuthLayoutProps {
  children: React.ReactNode;
  title: string;
  subtitle: string;
  onNavigateHome?: () => void;
}

export const AuthLayout: React.FC<AuthLayoutProps> = ({
  children,
  title,
  subtitle,
  onNavigateHome
}) => {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8 relative overflow-hidden">
      {/* Background Decorative Gradients */}
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-primary/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute top-1/2 -right-40 w-96 h-96 bg-secondary/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-40 left-1/3 w-80 h-80 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />

      {/* Header Logo */}
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center z-10">
        <button
          type="button"
          onClick={onNavigateHome}
          className="inline-flex items-center gap-3 cursor-pointer group focus:outline-none transition-transform hover:scale-105"
        >
          <div className="bg-gradient-to-tr from-primary to-primary-container text-white w-11 h-11 rounded-2xl flex items-center justify-center font-display font-black text-2xl shadow-lg shadow-primary/25 group-hover:rotate-6 transition-all">
            L
          </div>
          <span className="font-display font-black text-3xl text-primary tracking-tight">
            Learnova
          </span>
        </button>

        <h2 className="mt-6 text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
          {title}
        </h2>
        <p className="mt-2 text-sm text-slate-600 max-w-sm mx-auto">
          {subtitle}
        </p>
      </div>

      {/* Main Card Container */}
      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md z-10 px-4 sm:px-0">
        <div className="bg-white py-8 px-6 sm:px-10 shadow-xl shadow-slate-200/60 rounded-3xl border border-slate-100 backdrop-blur-sm">
          {children}
        </div>

        {/* Footer Note */}
        <div className="mt-6 text-center">
          <p className="text-xs text-slate-400">
            &copy; {new Date().getFullYear()} Learnova E-Learning Platform. Bảo mật chuẩn JWT & Cloud Security.
          </p>
        </div>
      </div>
    </div>
  );
};
