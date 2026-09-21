import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { Auth } from '../services/auth';
import { authGuard } from './auth-guard';

describe('authGuard', () => {
  const run = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  it('redirects to /login when there is no session', () => {
    const result = run() as UrlTree;
    expect(result.toString()).toBe('/login');
  });

  it('lets a logged user through', () => {
    TestBed.inject(Auth).login({ username: 'ana', rol: 'MUNICIPAL' });
    expect(run()).toBe(true);
  });
});
