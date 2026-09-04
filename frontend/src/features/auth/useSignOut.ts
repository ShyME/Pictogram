import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { signOut } from './authApi';

type SignOut = {
  signOut: () => void;
  signingOut: boolean;
};

export function useSignOut(): SignOut {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [signingOut, setSigningOut] = useState(false);

  async function run() {
    setSigningOut(true);
    await signOut();
    queryClient.clear();
    await navigate('/login', { replace: true });
  }

  return { signOut: () => void run(), signingOut };
}
