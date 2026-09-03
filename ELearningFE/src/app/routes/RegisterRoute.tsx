import { useNavigate } from 'react-router-dom';
import { RegisterPage } from '../../features/auth/pages/RegisterPage';

export function RegisterRoute() {
  const navigate = useNavigate();

  return (
    <RegisterPage
      onNavigateToLogin={() => navigate('/login')}
      onNavigateToHome={() => navigate('/courses')}
    />
  );
}
