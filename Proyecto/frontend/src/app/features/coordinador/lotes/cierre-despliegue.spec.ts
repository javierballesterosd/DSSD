import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { CierreDespliegue } from './cierre-despliegue';
import { Emergencias } from '../../../core/services/emergencias';
import { CierreDespliegueResponse } from '../../../core/models/emergencia';

describe('Cierre del despliegue', () => {
  const base: CierreDespliegueResponse = {
    emergenciaId: 7, municipio: 'La Plata', zonaAfectada: 'Centro', descripcion: 'Emergencia de ejemplo',
    actividadesTotales: 2, actividadesPendientes: 0, adjudicada: true, validacionRegistrada: false,
    finalizada: false, descripcionCierre: null, fechaValidacion: null, fechaFinalizacion: null,
  };
  let estado: CierreDespliegueResponse;
  let api: { cierre: ReturnType<typeof vi.fn>; validarDespliegue: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    estado = { ...base };
    api = { cierre: vi.fn(() => of(estado)), validarDespliegue: vi.fn(() => of(undefined)) };
    TestBed.configureTestingModule({ imports: [CierreDespliegue], providers: [
      provideRouter([]), { provide: Emergencias, useValue: api },
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } } },
    ] });
  });

  it('no permite validar con actividades pendientes', () => {
    estado.actividadesPendientes = 1;
    const fixture = TestBed.createComponent(CierreDespliegue); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#motivo-cierre').textContent).toContain('1 actividad pendiente');
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    expect(api.validarDespliegue).not.toHaveBeenCalled();
  });

  it('muestra el botón deshabilitado y la razón en rojo sin adjudicación', () => {
    estado.adjudicada = false;
    const fixture = TestBed.createComponent(CierreDespliegue); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    const motivo = fixture.nativeElement.querySelector('#motivo-cierre');
    expect(motivo.classList.contains('text-danger')).toBe(true);
    expect(motivo.textContent).toContain('todavía no confirmó');
  });

  it('distingue una emergencia sin actividades de una con actividades pendientes', () => {
    estado.actividadesTotales = 0;
    const fixture = TestBed.createComponent(CierreDespliegue); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('#motivo-cierre').textContent).toContain('no tiene actividades');
  });

  it('exige una descripción y confirmación, y envía la descripción al backend', () => {
    const fixture = TestBed.createComponent(CierreDespliegue); fixture.detectChanges();
    const component = fixture.componentInstance;
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    component['form'].setValue({ descripcionCierre: 'Todas las actividades completadas.', confirmacion: false });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    component['form'].controls.confirmacion.setValue(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(false);
    component['validar']();
    expect(api.validarDespliegue).toHaveBeenCalledWith(7, 'Todas las actividades completadas.');
    expect(component['cierre']()?.finalizada).toBe(false);
  });

  it('conserva la descripción cuando falla el envío y permite reintentar', () => {
    api.validarDespliegue.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 502,
      error: { mensaje: 'No se pudo completar la operación.' } })));
    const fixture = TestBed.createComponent(CierreDespliegue); fixture.detectChanges();
    const component = fixture.componentInstance;
    component['form'].setValue({ descripcionCierre: 'Descripción que debe conservarse.', confirmacion: true });
    component['validar'](); fixture.detectChanges();
    expect(component['form'].controls.descripcionCierre.value).toBe('Descripción que debe conservarse.');
    expect(fixture.nativeElement.textContent).toContain('No se pudo completar la operación.');
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(false);
  });

  it('muestra la descripción guardada y no permite cerrar nuevamente', () => {
    estado = { ...base, finalizada: true, validacionRegistrada: true,
      descripcionCierre: 'Asistencia completada y recursos liberados.', fechaFinalizacion: '2026-11-20T12:00:00' };
    const fixture = TestBed.createComponent(CierreDespliegue); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(estado.descripcionCierre);
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(api.validarDespliegue).not.toHaveBeenCalled();
  });
});
