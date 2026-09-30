import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Usuario } from '../models/usuario';
import { Auth } from './auth';

const ANA: Usuario = {
  userId: '7',
  username: 'ong.cruzroja',
  firstName: 'Ana',
  lastName: 'Gómez',
  role: 'ONG',
  group: 'CruzRojaLaPlata',
  ongId: 1,
  ongNombre: 'Cruz Roja La Plata',
};

describe('Auth', () => {
  let service: Auth;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(Auth);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('starts logged out and goes to /login', () => {
    expect(service.isLoggedIn()).toBe(false);
    expect(service.homeUrl()).toBe('/login');
  });

  it('login sets the user, its role and the home of that role', () => {
    service.login({ username: 'ong.cruzroja', password: 'bpm' }).subscribe();
    const req = http.expectOne((r) => r.url.endsWith('/auth/login'));
    expect(req.request.method).toBe('POST');
    req.flush(ANA);

    expect(service.isLoggedIn()).toBe(true);
    expect(service.rol()).toBe('ONG');
    expect(service.homeUrl()).toBe('/ong');
    expect(service.usuario()?.ongId).toBe(1);
  });

  it('logout clears the session', () => {
    service.login({ username: 'ong.cruzroja', password: 'bpm' }).subscribe();
    http.expectOne((r) => r.url.endsWith('/auth/login')).flush(ANA);

    service.logout().subscribe();
    http.expectOne((r) => r.url.endsWith('/auth/logout')).flush(null);

    expect(service.isLoggedIn()).toBe(false);
    expect(service.sessionStatus()).toBe('anonymous');
  });

  it('restoreSession leaves the user anonymous on 401', () => {
    service.restoreSession().subscribe((usuario) => expect(usuario).toBeNull());
    http
      .expectOne((r) => r.url.endsWith('/auth/me'))
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(service.isLoggedIn()).toBe(false);
    expect(service.sessionStatus()).toBe('anonymous');
  });
});
