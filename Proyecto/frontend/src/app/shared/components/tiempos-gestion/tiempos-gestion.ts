import { Component, computed, input } from '@angular/core';

import { DashboardTiempo } from '@core/models/dashboard';

@Component({
  selector: 'app-tiempos-gestion',
  templateUrl: './tiempos-gestion.html',
  styleUrl: './tiempos-gestion.scss',
})
export class TiemposGestion {
  readonly titulo = input<string>('Tiempos de gestión');
  readonly tiempos = input<DashboardTiempo[]>([]);
  readonly cargando = input<boolean>(false);

  protected readonly maximo = computed(() => {
    const valores = this.tiempos()
      .map((tiempo) => tiempo.promedioSegundos ?? 0)
      .filter((valor) => valor > 0);

    return Math.max(...valores, 0);
  });

  protected porcentaje(segundos: number | null): number {
    if (segundos === null || this.maximo() === 0) {
      return 0;
    }

    return (segundos / this.maximo()) * 100;
  }

  protected formatearDuracion(segundos: number | null): string {
    if (segundos === null) {
      return 'Sin datos';
    }

    if (segundos < 0) {
      return 'Dato inválido';
    }

    const dias = Math.floor(segundos / 86400);
    const horas = Math.floor((segundos % 86400) / 3600);
    const minutos = Math.floor((segundos % 3600) / 60);
    const segundosRestantes = Math.floor(segundos % 60);

    if (dias > 0) {
      return `${dias} d ${horas} h`;
    }

    if (horas > 0) {
      return `${horas} h ${minutos} min`;
    }

    if (minutos > 0) {
      return `${minutos} min`;
    }

    return `${segundosRestantes} seg`;
  }
}
