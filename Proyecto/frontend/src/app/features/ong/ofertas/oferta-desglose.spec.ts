import { TestBed } from '@angular/core/testing';
import { AporteRecurso } from '../../../core/models/oferta';
import { OfertaDesglose } from './oferta-desglose';

function aporte(extra: Partial<AporteRecurso> = {}, aceptadaBomberos?: number): AporteRecurso {
  return {
    itemLoteId: 1,
    recursoNombre: 'Paramédicos',
    unidadMedida: 'personas',
    cantidadRequerida: 6,
    totalOfrecido: 8,
    porOng: [
      { ongId: 3, razonSocial: 'Bomberos', cantidadOfrecida: 4, cantidadAceptada: aceptadaBomberos },
      { ongId: 1, razonSocial: 'Cruz Roja', cantidadOfrecida: 4, cantidadAceptada: aceptadaBomberos == null ? undefined : 4 },
    ],
    ...extra,
  };
}

function texto(aportes: AporteRecurso[]): string {
  const fixture = TestBed.createComponent(OfertaDesglose);
  fixture.componentRef.setInput('aportes', aportes);
  fixture.detectChanges();
  return (fixture.nativeElement as HTMLElement).textContent ?? '';
}

describe('OfertaDesglose', () => {
  it('muestra cuánto se aceptó de cada ONG y el total aceptado cuando la oferta fue validada', () => {
    const contenido = texto([aporte({ totalAceptado: 6 }, 2)]);

    expect(contenido).toContain('Aceptado');
    expect(contenido).toContain('aceptado: 2');
    expect(contenido).toContain('aceptado: 4');
    expect(contenido).toContain('6 personas');
  });

  it('no muestra la columna ni las cantidades aceptadas mientras la oferta está pendiente', () => {
    const contenido = texto([aporte()]);

    expect(contenido).not.toContain('Aceptado');
    expect(contenido).not.toContain('aceptado:');
  });
});
