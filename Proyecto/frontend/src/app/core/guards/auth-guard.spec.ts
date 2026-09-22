import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, provideRouter } from '@angular/router';
import { Auth } from '../services/auth';
import { authGuard } from './auth-guard';

// TODO: el guard está bypaseado (AUTH_HABILITADO = false en auth-guard.ts) hasta que
// exista el login real contra Bonita. Mientras tanto siempre deja pasar; cuando se
// reactive, volver a los tests de "redirige sin sesión" / "deja pasar logueado".
describe('authGuard', () => {
  const run = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  it('lets everyone through while el login no está implementado', () => {
    expect(run()).toBe(true);
  });

  it('also lets a logged user through', () => {
    TestBed.inject(Auth).login({ username: 'ana', rol: 'MUNICIPAL' });
    expect(run()).toBe(true);
  });
});
