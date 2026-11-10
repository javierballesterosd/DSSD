import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DashboardService } from './dashboard';

describe('DashboardService', () => {
  let service: DashboardService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(DashboardService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envia municipio, gravedad, recurso, ONG y fechas en todas las consultas de Bonita', () => {
    const filtros = { municipioId: 2, recursoId: 3, ongId: 4, gravedad: 'ALTA' as const, desde: '2026-10-01', hasta: '2026-10-31' };
    service.etapas(filtros).subscribe();
    service.tiempoValidacionNacional(filtros).subscribe();
    service.tiempoDecisionMunicipal(filtros).subscribe();
    service.tiempoCompromiso(filtros).subscribe();
    service.tiempoEmergenciasFinalizadas(filtros).subscribe();
    service.seguimiento(filtros).subscribe();
    const peticiones = http.match(() => true);
    expect(peticiones).toHaveLength(6);
    expect(peticiones[0].request.url).toMatch(/\/etapas\/ranking$/);
    for (const peticion of peticiones) {
      expect(peticion.request.params.get('municipioId')).toBe('2');
      expect(peticion.request.params.get('recursoId')).toBe('3');
      expect(peticion.request.params.get('ongId')).toBe('4');
      expect(peticion.request.params.get('gravedad')).toBe('ALTA');
      expect(peticion.request.params.get('desde')).toBe('2026-10-01');
      expect(peticion.request.params.get('hasta')).toBe('2026-10-31');
      peticion.flush(peticion.request.url.endsWith('/ranking') || peticion.request.url.endsWith('/seguimiento') ? [] : {});
    }
  });

  it('omite los filtros cuando se consulta sin seleccion', () => {
    service.etapas().subscribe();
    const peticion = http.expectOne((r) => r.url.endsWith('/etapas/ranking'));
    expect(peticion.request.params.has('municipioId')).toBe(false);
    expect(peticion.request.params.has('recursoId')).toBe(false);
    expect(peticion.request.params.has('ongId')).toBe(false);
    expect(peticion.request.params.has('gravedad')).toBe(false);
    peticion.flush([]);
  });
});
