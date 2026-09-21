import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { Rol } from '../models/rol';
import { Auth } from '../services/auth';
import { roleGuard } from './role-guard';

describe('roleGuard', () => {
  const run = (...roles: Rol[]) =>
    TestBed.runInInjectionContext(() =>
      roleGuard(...roles)({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  it('lets through a user with the required role', () => {
    TestBed.inject(Auth).login({ username: 'ana', rol: 'ONG' });
    expect(run('ONG')).toBe(true);
  });

  it('sends a user with another role to their own home', () => {
    TestBed.inject(Auth).login({ username: 'ana', rol: 'ONG' });
    expect((run('AUDITOR') as UrlTree).toString()).toBe('/ong');
  });

  it('sends an anonymous user to /login', () => {
    expect((run('ONG') as UrlTree).toString()).toBe('/login');
  });
});
