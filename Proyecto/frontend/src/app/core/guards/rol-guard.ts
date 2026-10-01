import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from '../models/rol';
import { Auth } from '../services/auth';

/** Deja pasar solo al perfil indicado; a los demás los manda al inicio de su propio perfil. */
export const rolGuard =
  (rol: Rol): CanActivateFn =>
  () => {
    const auth = inject(Auth);
    return auth.rol() === rol || inject(Router).createUrlTree([auth.homeUrl()]);
  };
