import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from '../services/auth';

/** Deja pasar solo a usuarios logueados; si no, redirige a /login. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(Auth);
  return auth.isLoggedIn() || inject(Router).createUrlTree(['/login']);
};
