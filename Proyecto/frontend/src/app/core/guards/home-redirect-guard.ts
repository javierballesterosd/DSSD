import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from '../services/auth';

/** Para la ruta raíz: redirige al home del rol del usuario. */
export const homeRedirectGuard: CanActivateFn = () =>
  inject(Router).createUrlTree([inject(Auth).homeUrl()]);
