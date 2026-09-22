import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from '../services/auth';

// TODO: en false hasta que el login contra Bonita esté implementado. Hoy no hay
// forma de loguearse de verdad, así que auth.isLoggedIn() nunca se vuelve true
// y este guard mandaría a todos a /login sin salida.
const AUTH_HABILITADO = false;

/** Deja pasar solo a usuarios logueados; si no, redirige a /login. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(Auth);
  return (
    !AUTH_HABILITADO ||
    auth.isLoggedIn() ||
    inject(Router).createUrlTree(['/login'])
  );
};
