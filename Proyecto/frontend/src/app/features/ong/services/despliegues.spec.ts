import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { Despliegues } from './despliegues';

describe('Despliegues', () => {
  let service: Despliegues;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(Despliegues);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('pide mis despliegues con GET a /api/despliegues/mios', () => {
    service.misDespliegues().subscribe((lista) => expect(lista).toEqual([]));

    const req = httpMock.expectOne(`${environment.apiUrl}/despliegues/mios`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('marca la actividad de la oferta con POST a /api/ofertas/{id}/actividades/{id}/finalizacion', () => {
    service.finalizarActividad(20, 9).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/ofertas/20/actividades/9/finalizacion`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});
