import { inject } from '@angular/core';
import { Router, type CanActivateFn } from '@angular/router';
import { Auth } from '../auth/auth';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(Auth);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  router.navigate(['/signin']);
  return false;
};

export const adminGuard: CanActivateFn = (route, state) => {
  const auth = inject(Auth);
  const router = inject(Router);

  if (auth.isAuthenticated() && auth.currentUser()?.role === 'ADMIN') {
    return true;
  }

  router.navigate(['/signin']);
  return false;
};
