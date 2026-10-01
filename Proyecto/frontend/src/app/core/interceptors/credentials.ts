import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';

/**
 * Manda la cookie de sesión (JSESSIONID) en toda llamada al backend: el backend guarda la
 * sesión de Bonita en la HttpSession y la usa para saber quién está logueado.
 */
export const credentialsInterceptor: HttpInterceptorFn = (req, next) =>
  next(req.url.startsWith(environment.apiUrl) ? req.clone({ withCredentials: true }) : req);
