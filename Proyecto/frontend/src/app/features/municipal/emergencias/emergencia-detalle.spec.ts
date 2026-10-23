import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { EmergenciaDetalle } from './emergencia-detalle';

const EMERGENCIA = {
  id: 7,
  descripcion: 'Inundación en el centro',
  nivelGravedad: 'ALTA',
  nivelGravedadEtiqueta: 'Alta',
  zonaAfectada: 'Centro',
  municipio: 'La Plata',
  municipioId: 1,
  fechaRegistro: '2026-10-22T10:00:00',
  loteId: 10,
  estadoLote: 'ACTIVO',
  requiereActualizacion: false,
};
const RECURSOS = {
  loteId: 10,
  decision: null,
  confirmada: false,
  puedeDecidir: true,
  mensaje: null,
  recursos: [{ recursoId: 3, nombre: 'Frazadas', unidadMedida: 'unidades', cantidad: 40 }],
};

describe('Decisión municipal del conjunto de ofertas', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [EmergenciaDetalle],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  async function crear(resumen = RECURSOS, emergencia = EMERGENCIA) {
    const fixture = TestBed.createComponent(EmergenciaDetalle);
    await fixture.whenStable();
    http.expectOne((r) => r.url.endsWith('/emergencias/7')).flush(emergencia);
    http.expectOne((r) => r.url.endsWith('/recursos-aceptados')).flush(resumen);
    await fixture.whenStable();
    return fixture;
  }

  it('muestra cantidades aceptadas y una decisión para todo el conjunto', async () => {
    const fixture = await crear();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('tbody tr')).toHaveLength(1);
    expect(el.querySelector('tbody')?.textContent).toContain('40');
    expect(el.querySelectorAll('input[type=checkbox]')).toHaveLength(0);
    const aceptar = Array.from(el.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Aceptar todos'),
    )!;
    aceptar.click();
    await fixture.whenStable();
    expect(el.querySelector('app-confirm-modal')).not.toBeNull();
    Array.from(el.querySelectorAll('app-confirm-modal button'))
      .find((b) => b.textContent?.includes('Confirmar'))!
      .dispatchEvent(new MouseEvent('click'));
    const req = http.expectOne((r) => r.url.endsWith('/decision-municipal'));
    expect(req.request.body).toEqual({ loteId: 10, aceptar: true });
    req.flush({ mensaje: 'Error de SN' }, { status: 502, statusText: 'Bad Gateway' });
    http
      .expectOne((r) => r.url.endsWith('/recursos-aceptados'))
      .flush({ ...RECURSOS, decision: true });
    await fixture.whenStable();
    expect(el.textContent).toContain('Reintentar aceptación');
    expect(el.textContent).not.toContain('Rechazar todas las ofertas');
  });

  it('requiere actualizar la misma emergencia después de rechazar', async () => {
    const fixture = await crear(
      { ...RECURSOS, puedeDecidir: false, confirmada: true },
      { ...EMERGENCIA, requiereActualizacion: true },
    );
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('a[href="/municipal/emergencias/7/actualizar"]')).not.toBeNull();
    expect(el.textContent).toContain('Debés actualizar la emergencia');
  });

  it('permite reiniciar por rechazo si el SN no habilitó ningún recurso', async () => {
    const fixture = await crear({ ...RECURSOS, recursos: [] });
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('El SN no habilitó recursos');
    expect(el.textContent).toContain('Rechazar todas las ofertas');
    expect(el.textContent).not.toContain('Aceptar todos los recursos');
  });
});
