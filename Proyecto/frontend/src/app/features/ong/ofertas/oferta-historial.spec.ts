import { AporteRecurso } from '../../../core/models/oferta';
import { diferencias } from './oferta-historial';

function aporte(itemLoteId: number, porOng: [number, number][]): AporteRecurso {
  return {
    itemLoteId,
    recursoNombre: `Recurso ${itemLoteId}`,
    unidadMedida: 'u',
    cantidadRequerida: 1000,
    totalOfrecido: 0,
    porOng: porOng.map(([ongId, cantidadOfrecida]) => ({
      ongId,
      razonSocial: `ONG ${ongId}`,
      cantidadOfrecida,
    })),
  };
}

describe('diferencias entre versiones', () => {
  it('detecta cantidades cambiadas, celdas agregadas y quitadas', () => {
    const anterior = [
      aporte(1, [
        [1, 500],
        [2, 200],
      ]),
    ];
    const actual = [aporte(1, [[1, 600]]), aporte(2, [[2, 50]])];

    expect(diferencias(anterior, actual)).toEqual([
      'Recurso 1 · ONG 1: 500 → 600',
      'Recurso 2 · ONG 2: agregado (50)',
      'Recurso 1 · ONG 2: quitado (200)',
    ]);
  });

  it('no informa nada si las versiones son iguales', () => {
    const aportes = [aporte(1, [[1, 500]])];

    expect(diferencias(aportes, aportes)).toEqual([]);
  });
});
