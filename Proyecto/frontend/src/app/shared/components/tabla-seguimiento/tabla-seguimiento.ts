import { Component, input } from '@angular/core';

import { DashboardSeguimiento } from '@core/models/dashboard';

@Component({
  selector: 'app-tabla-seguimiento',
  templateUrl: './tabla-seguimiento.html',
  styleUrl: './tabla-seguimiento.scss',
})
export class TablaSeguimiento {
  readonly emergencias = input<DashboardSeguimiento[]>([]);
  readonly cargando = input<boolean>(false);

  protected estadoClase(estado: string): string {
    const normalizado = estado.trim().toUpperCase();

    switch (normalizado) {
      case 'ACTIVO':
      case 'ACTIVE':
        return 'text-bg-success';

      case 'FINALIZADO':
      case 'COMPLETADO':
        return 'text-bg-primary';

      case 'CANCELADO':
      case 'ABORTADO':
        return 'text-bg-danger';

      default:
        return 'text-bg-secondary';
    }
  }
}
