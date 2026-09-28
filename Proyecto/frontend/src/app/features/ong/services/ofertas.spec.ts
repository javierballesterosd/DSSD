import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { OfertaRequest } from '../../../core/models/oferta';
import { Ofertas } from './ofertas';

describe('Ofertas', () => {
  let service: Ofertas;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(Ofertas);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('hace POST a /api/ofertas con ongId en cada detalle', () => {
    const request: OfertaRequest = {
      loteId: 10,
      ongIds: [1, 2],
      detalles: [
        { itemLoteId: 200, ongId: 1, cantidadOfrecida: 800 },
        { itemLoteId: 200, ongId: 2, cantidadOfrecida: 200 },
      ],
    };

    service.registrar(request).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/ofertas`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    for (const detalle of req.request.body.detalles) {
      expect(detalle.ongId).toBeDefined();
    }
    req.flush({});
  });

  it('pide mis ofertas del lote con loteId como parámetro', () => {
    service.misOfertas(10).subscribe((ofertas) => expect(ofertas).toEqual([]));

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.apiUrl}/ofertas/mias` && r.params.get('loteId') === '10',
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
