import { Service, computed, signal } from '@angular/core';
import { ROL_HOME, ROLES, Rol } from '../models/rol';
import { Usuario } from '../models/usuario';

const STORAGE_KEY = 'rescuesync.usuario';

@Service()
export class Auth {
  private readonly _usuario = signal<Usuario | null>(this.leerSesion());

  readonly usuario = this._usuario.asReadonly();
  readonly rol = computed<Rol | null>(() => this._usuario()?.rol ?? null);
  readonly isLoggedIn = computed(() => this._usuario() !== null);

  /** Ruta de inicio del usuario logueado; /login si no hay sesión. */
  readonly homeUrl = computed(() => {
    const rol = this.rol();
    return rol ? ROL_HOME[rol] : '/login';
  });


  login(credentials: LoginRequest): Observable<Usuario> {
    console.log('Nombre usuario: ', credentials.username, 'contraseña: ', credentials.password);
    return this.http.post<Usuario>(
      `${this.apiUrl}/api/auth/login`, credentials, { withCredentials: true })
      .pipe(tap((usuario) => this._usuario.set(usuario)));
  }

  logout(): void {
    this._usuario.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  private leerSesion(): Usuario | null {
    try {
      const guardado = localStorage.getItem(STORAGE_KEY);
      if (!guardado) return null;
      const usuario = JSON.parse(guardado) as Usuario;
      return ROLES.includes(usuario.rol) ? usuario : null;
    } catch {
      return null;
    }
  }
}
