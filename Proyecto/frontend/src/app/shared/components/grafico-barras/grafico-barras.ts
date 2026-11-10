import { DecimalPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import { DashboardRankingResponse } from '@core/models/dashboard';

@Component({
  selector: 'app-grafico-barras',
  templateUrl: './grafico-barras.html',
  styleUrl: './grafico-barras.scss',
})
export class GraficoBarras {
  readonly titulo = input.required<string>();
  readonly subtitulo = input<string>('');
  readonly datos = input<DashboardRankingResponse[]>([]);
  readonly unidad = input<string>('');
  readonly color = input<string>('var(--bs-primary)');

  protected readonly maximo = computed(() => {
    const valores = this.datos().map((dato) => dato.total);

    return Math.max(...valores, 0);
  });

  protected porcentaje(total: number): number {
    const maximo = this.maximo();

    if (maximo === 0) {
      return 0;
    }

    return (total / maximo) * 100;
  }

  protected valorFormateado(dato: DashboardRankingResponse): string {
    const unidad = dato.unidadMedida || this.unidad();

    return unidad ? `${dato.total} ${unidad}` : dato.total.toLocaleString('es-AR');
  }
}
