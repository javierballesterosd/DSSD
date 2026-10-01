import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  provideRouter,
} from '@angular/router';
import { Auth } from '../services/auth';
import { authGuard } from './auth-guard';

describe('authGuard', () => {
  const run = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it('redirects to /login without a session', () => {
    const resultado = run();
    expect(TestBed.inject(Router).serializeUrl(resultado as never)).toBe('/login');
  });

  it('lets a logged user through', () => {
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

    expect(run()).toBe(true);
  });
});
