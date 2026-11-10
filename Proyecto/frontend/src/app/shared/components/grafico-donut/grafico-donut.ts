import { DecimalPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import { DashboardRankingResponse } from '@core/models/dashboard';

interface SegmentoDonut {
  id: number;
  nombre: string;
  total: number;
  porcentaje: number;
  color: string;
  dasharray: string;
  dashoffset: number;
}

@Component({
  selector: 'app-grafico-donut',
  imports: [DecimalPipe],
  templateUrl: './grafico-donut.html',
  styleUrl: './grafico-donut.scss',
})
export class GraficoDonut {
  readonly titulo = input.required<string>();
  readonly subtitulo = input<string>('');
  readonly datos = input<DashboardRankingResponse[]>([]);

  readonly colores = input<string[]>([
    '#198754',
    '#0d6efd',
    '#ffc107',
    '#dc3545',
    '#6f42c1',
    '#0dcaf0',
    '#fd7e14',
    '#6c757d',
  ]);

  private readonly radio = 42;
  private readonly circunferencia = 2 * Math.PI * this.radio;

  protected readonly total = computed(() =>
    this.datos().reduce((acumulado, dato) => acumulado + dato.total, 0),
  );

  protected readonly segmentos = computed<SegmentoDonut[]>(() => {
    const datos = this.datos();
    const total = this.total();
    const colores = this.colores();

    if (total <= 0) {
      return [];
    }

    let desplazamiento = 0;

    return datos.map((dato, indice) => {
      const porcentaje = (dato.total / total) * 100;
      const longitud = (porcentaje / 100) * this.circunferencia;

      const segmento: SegmentoDonut = {
        id: dato.id,
        nombre: dato.nombre,
        total: dato.total,
        porcentaje,
        color: colores[indice % colores.length],
        dasharray: `${longitud} ${this.circunferencia - longitud}`,
        dashoffset: -desplazamiento,
      };

      desplazamiento += longitud;

      return segmento;
    });
  });

  protected formatearPorcentaje(valor: number): string {
    return `${valor.toLocaleString('es-AR', {
      maximumFractionDigits: 1,
    })}%`;
  }
}
