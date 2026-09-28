import { InventarioOng } from '../../../core/models/ong';

export type EstadoCelda = 'sin-recurso' | 'ok' | 'excede-inventario' | 'invalida';

/** Indexa el inventario por `${ongId}:${recursoId}` para lookup O(1) desde la grilla. */
export function indexarInventario(inventarios: InventarioOng[]): Map<string, number> {
  const indice = new Map<string, number>();
  for (const inventario of inventarios) {
    for (const recurso of inventario.recursos) {
      indice.set(`${inventario.ongId}:${recurso.recursoId}`, recurso.cantidadDisponible);
    }
  }
  return indice;
}

export function estadoCelda(disponible: number | undefined, cantidad: number | null): EstadoCelda {
  if (disponible === undefined || disponible <= 0) {
    return 'sin-recurso';
  }
  if (cantidad === null || cantidad === undefined) {
    return 'ok';
  }
  if (!Number.isInteger(cantidad) || cantidad < 0) {
    return 'invalida';
  }
  if (cantidad > disponible) {
    return 'excede-inventario';
  }
  return 'ok';
}

export function totalFila(aportes: (number | null)[]): number {
  return aportes.reduce<number>((suma, valor) => suma + (valor ?? 0), 0);
}

export function excedeSolicitado(total: number, requerida: number): boolean {
  return total > requerida;
}

/** ids de las ONGs seleccionadas que no aportan nada (todas sus celdas vacías o en cero). */
export function ongsSinAporte(ongIds: number[], valores: Map<number, number[]>): number[] {
  return ongIds.filter((ongId) => {
    const cantidades = valores.get(ongId) ?? [];
    return cantidades.every((cantidad) => !cantidad || cantidad <= 0);
  });
}

/** Sugerencias cortas para el datalist de una celda: no lista 1..disponible entero. */
export function sugerencias(disponible: number, requerida: number): number[] {
  if (disponible <= 0) {
    return [];
  }
  const candidatos = new Set<number>([
    Math.min(requerida, disponible),
    disponible,
    Math.round(disponible * 0.25),
    Math.round(disponible * 0.5),
    Math.round(disponible * 0.75),
  ]);
  return [...candidatos]
    .filter((valor) => valor > 0 && valor <= disponible)
    .sort((a, b) => a - b)
    .slice(0, 8);
}
