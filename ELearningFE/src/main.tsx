import { ConfirmProvider } from './app/context/ConfirmContext';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import './index.css';
import './App.css';
import App from './App.tsx';
import { AuthProvider } from './app/context/AuthContext';
import { ToastProvider } from './app/context/ToastContext';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
          <ConfirmProvider><App /></ConfirmProvider>
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  </StrictMode>
);
