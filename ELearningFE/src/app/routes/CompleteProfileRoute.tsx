import { useNavigate } from 'react-router-dom';
import { CompleteProfilePage } from '../../features/auth/pages/CompleteProfilePage';

export function CompleteProfileRoute() {
  const navigate = useNavigate();

  return (
    <CompleteProfilePage
      onCompleteSuccess={() => navigate('/profile')}
      onSkip={() => navigate('/courses')}
    />
  );
}
