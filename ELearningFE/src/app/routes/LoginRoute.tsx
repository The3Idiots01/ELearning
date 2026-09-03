import { useNavigate } from 'react-router-dom';
import { LoginPage } from '../../features/auth/pages/LoginPage';

export function LoginRoute() {
  const navigate = useNavigate();

  return (
    <LoginPage
      onNavigateToRegister={() => navigate('/register')}
      onNavigateToHome={() => navigate('/courses')}
      onLoginSuccess={() => navigate('/courses')}
    />
  );
}
