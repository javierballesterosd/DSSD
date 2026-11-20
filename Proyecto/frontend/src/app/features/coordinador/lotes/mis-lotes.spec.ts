import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MisLotes } from './mis-lotes';
import { Lotes } from '../../../core/services/lotes';
import { Auth } from '../../../core/services/auth';

describe('Filtro de lotes pendientes de validación', () => {
  let listarMios: ReturnType<typeof vi.fn>;
  beforeEach(() => {
    listarMios = vi.fn(() => of([]));
    TestBed.configureTestingModule({ imports: [MisLotes], providers: [provideRouter([]),
      { provide: Lotes, useValue: { listarMios } },
      { provide: Auth, useValue: { usuario: () => ({ regionNombre: 'Norte' }) } },
    ] });
  });

  it('consulta todos inicialmente y los pendientes al elegir el filtro', () => {
    const fixture = TestBed.createComponent(MisLotes); fixture.detectChanges();
    expect(listarMios).toHaveBeenLastCalledWith(false);
    const selector: HTMLSelectElement = fixture.nativeElement.querySelector('#filtro-validacion');
    selector.value = 'pendientes'; selector.dispatchEvent(new Event('change')); fixture.detectChanges();
    expect(listarMios).toHaveBeenLastCalledWith(true);
    expect(fixture.nativeElement.textContent).toContain('No hay emergencias pendientes de validación en tu región.');
    selector.value = 'todos'; selector.dispatchEvent(new Event('change')); fixture.detectChanges();
    expect(listarMios).toHaveBeenLastCalledWith(false);
  });

  it('lleva directamente a validar el cierre desde un resultado pendiente', () => {
    listarMios.mockReturnValue(of([{
      id: 25, titulo: 'Lote para validar', estado: 'ACTIVO', fechaCreacion: '2026-11-20T10:00:00',
      fechaCierreOfertas: null, items: [], emergencia: {
        id: 50, municipio: 'La Plata', zonaAfectada: 'Centro', descripcion: 'Emergencia de ejemplo',
        nivelGravedad: 'MEDIA', nivelGravedadEtiqueta: 'Media',
      },
    }]));
    const fixture = TestBed.createComponent(MisLotes); fixture.detectChanges();
    const selector: HTMLSelectElement = fixture.nativeElement.querySelector('#filtro-validacion');
    selector.value = 'pendientes'; selector.dispatchEvent(new Event('change')); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-lote-tarjeta a').getAttribute('href'))
      .toBe('/coordinador/emergencias/50/cierre');
  });
});
