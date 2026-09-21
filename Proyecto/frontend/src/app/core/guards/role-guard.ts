import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from '../models/rol';
import { Auth } from '../services/auth';

/** Uso: `canActivate: [roleGuard('ONG')]`. Si el rol no coincide, vuelve al home del usuario. */
export const roleGuard =
  (...roles: Rol[]): CanActivateFn =>
  () => {
    const auth = inject(Auth);
    const rol = auth.rol();
    return (rol !== null && roles.includes(rol)) || inject(Router).createUrlTree([auth.homeUrl()]);
  };
