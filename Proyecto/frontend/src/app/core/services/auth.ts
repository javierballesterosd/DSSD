import { Service, computed, signal, inject } from '@angular/core';
import { ROL_HOME, Rol } from '../models/rol';
import { Usuario } from '../models/usuario';
import { LoginRequest } from '@core/models/login-request';
import { catchError, Observable, of, tap, throwError } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment.development';

export type SessionStatus = 'loading' | 'authenticated' | 'anonymous';

@Service()
export class Auth {
  private readonly _usuario = signal<Usuario | null>(null);
  private readonly _sessionStatus = signal<SessionStatus>('loading');

  private readonly apiUrl = environment.apiUrl;

  private readonly http = inject(HttpClient);

  readonly usuario = this._usuario.asReadonly();
  readonly rol = computed<Rol | null>(() => this._usuario()?.role ?? null);
  readonly isLoggedIn = computed(() => this._usuario() !== null);
  readonly sessionStatus = this._sessionStatus.asReadonly();

  /** Ruta de inicio del usuario logueado; /login si no hay sesión. */
  readonly homeUrl = computed(() => {
    const rol = this.rol();
    return rol ? ROL_HOME[rol] : '/login';
  });

  login(credentials: LoginRequest): Observable<Usuario> {
    return this.http
      .post<Usuario>(`${this.apiUrl}/auth/login`, credentials, { withCredentials: true })
      .pipe(
        tap((usuario) => {
          this._usuario.set(usuario);
          this._sessionStatus.set('authenticated');
        }),
      );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/auth/logout`, {}, { withCredentials: true }).pipe(
      tap(() => {
        this._usuario.set(null);
        this._sessionStatus.set('anonymous');
      }),
    );
  }

  restoreSession(): Observable<Usuario | null> {
    return this.http.get<Usuario>(`${this.apiUrl}/auth/me`, { withCredentials: true }).pipe(
      tap((usuario) => this._usuario.set(usuario)),
      catchError((error) => {
        if (error.status === 401) {
          this._usuario.set(null);
          this._sessionStatus.set('anonymous');
          return of(null);
        }
        return throwError(() => error);
      }),
      tap((usuario) => {
        if (usuario) {
          this._sessionStatus.set('authenticated');
        }
      }),
    );
  }
}
