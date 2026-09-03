import { useNavigate } from 'react-router-dom';
import { ProfilePage } from '../../features/auth/pages/ProfilePage';

export function ProfileRoute() {
  const navigate = useNavigate();

  return (
    <ProfilePage
      onNavigateHome={() => navigate('/courses')}
      onNavigateToLogin={() => navigate('/login')}
      onNavigateToCompleteProfile={() => navigate('/complete-profile')}
    />
  );
}
