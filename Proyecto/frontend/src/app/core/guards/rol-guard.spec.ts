import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  provideRouter,
} from '@angular/router';
import { Rol } from '../models/rol';
import { Auth } from '../services/auth';
import { rolGuard } from './rol-guard';

describe('rolGuard', () => {
  const run = (rol: Rol) =>
    TestBed.runInInjectionContext(() =>
      rolGuard(rol)({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    TestBed.inject(Auth).login({ username: 'ana', password: 'bpm' }).subscribe();
    TestBed.inject(HttpTestingController)
      .expectOne((r) => r.url.endsWith('/auth/login'))
      .flush({
        userId: '1',
        username: 'ana',
        firstName: 'Ana',
        lastName: 'G',
        role: 'MUNICIPAL',
        group: 'Municipio',
      });
  });

  it('lets the matching role through', () => {
    expect(run('MUNICIPAL')).toBe(true);
  });

  it('redirects another role to its own home', () => {
    const resultado = run('COORDINADOR');

    expect(TestBed.inject(Router).serializeUrl(resultado as never)).toBe('/municipal');
  });
});
