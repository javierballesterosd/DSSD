import { InventarioOng } from '../../../core/models/ong';
import {
  estadoCelda,
  excedeSolicitado,
  indexarInventario,
  ongsSinAporte,
  sugerencias,
  totalFila,
} from './disponibilidad';

describe('indexarInventario', () => {
  it('indexa dos inventarios distintos por ong y recurso', () => {
    const inventarios: InventarioOng[] = [
      {
        ongId: 1,
        razonSocial: 'Cruz Solidaria',
        recursos: [
          {
            recursoId: 100,
            recursoNombre: 'Raciones',
            unidadMedida: 'raciones',
            cantidadDisponible: 800,
          },
        ],
      },
      {
        ongId: 2,
        razonSocial: 'Manos Unidas',
        recursos: [
          {
            recursoId: 100,
            recursoNombre: 'Raciones',
            unidadMedida: 'raciones',
            cantidadDisponible: 500,
          },
        ],
      },
    ];

    const indice = indexarInventario(inventarios);

    expect(indice.get('1:100')).toBe(800);
    expect(indice.get('2:100')).toBe(500);
    expect(indice.get('3:100')).toBeUndefined();
  });
});

describe('estadoCelda', () => {
  it('marca sin-recurso cuando no hay disponible', () => {
    expect(estadoCelda(undefined, null)).toBe('sin-recurso');
    expect(estadoCelda(0, null)).toBe('sin-recurso');
  });

  it('marca ok cuando la celda está vacía o dentro del inventario', () => {
    expect(estadoCelda(800, null)).toBe('ok');
    expect(estadoCelda(800, 500)).toBe('ok');
  });

  it('marca excede-inventario cuando supera el disponible', () => {
    expect(estadoCelda(800, 900)).toBe('excede-inventario');
  });

  it('marca invalida para valores negativos o no enteros', () => {
    expect(estadoCelda(800, -1)).toBe('invalida');
    expect(estadoCelda(800, 1.5)).toBe('invalida');
  });
});

describe('totalFila / excedeSolicitado', () => {
  it('suma las cantidades ignorando nulos', () => {
    expect(totalFila([800, null, 200])).toBe(1000);
  });

  it('detecta cuando el total supera lo solicitado', () => {
    expect(excedeSolicitado(1000, 900)).toBe(true);
    expect(excedeSolicitado(900, 1000)).toBe(false);
  });
});

describe('ongsSinAporte', () => {
  it('detecta las ongs seleccionadas cuyas celdas están todas en cero o vacías', () => {
    const valores = new Map<number, number[]>([
      [1, [800, 0]],
      [2, [0, 0]],
    ]);

    expect(ongsSinAporte([1, 2], valores)).toEqual([2]);
  });
});

describe('sugerencias', () => {
  it('no explota con disponibles grandes y devuelve como mucho 8 valores', () => {
    const resultado = sugerencias(5000, 1000);
    expect(resultado.length).toBeLessThanOrEqual(8);
    expect(resultado.every((valor) => valor > 0 && valor <= 5000)).toBe(true);
  });

  it('devuelve vacío cuando no hay disponible', () => {
    expect(sugerencias(0, 100)).toEqual([]);
  });
});
