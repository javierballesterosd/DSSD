import { TestBed } from '@angular/core/testing';
import { Auth } from './auth';

describe('Auth', () => {
  let service: Auth;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(Auth);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('starts logged out and goes to /login', () => {
    expect(service.isLoggedIn()).toBe(false);
    expect(service.homeUrl()).toBe('/login');
  });

  it('login sets the user, its single role and the home of that role', () => {
    service.login({ username: 'ana', rol: 'ONG' });
    expect(service.isLoggedIn()).toBe(true);
    expect(service.rol()).toBe('ONG');
    expect(service.homeUrl()).toBe('/ong');
  });

  it('logout clears the session', () => {
    service.login({ username: 'ana', rol: 'ONG' });
    service.logout();
    expect(service.isLoggedIn()).toBe(false);
  });
});
