import { DatePipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  EmergenciaParaLoteResponse,
  NIVEL_GRAVEDAD_BADGE,
  NIVEL_GRAVEDAD_COLOR,
} from '../../../core/models/emergencia';
import { ESTADO_LOTE_BADGE, ESTADO_LOTE_LABEL } from '../../../core/models/lote';
import { FECHA_LISTADO } from '../../formatos-fecha';

/**
 * Tarjeta de una emergencia para los listados, con el mismo armado que la de lotes; toda la tarjeta
 * lleva a `enlace`. El contenido proyectado va debajo de la descripción (ej. la acción disponible).
 */
@Component({
  imports: [RouterLink, DatePipe],
  selector: 'app-emergencia-tarjeta',
  host: { class: 'd-block h-100' },
  template: `
    @let e = emergencia();
    <a
      class="card h-100 overflow-hidden shadow-sm text-decoration-none text-body tarjeta-hover"
      [routerLink]="enlace()"
    >
      <div
        class="card-header border-0 d-flex justify-content-between align-items-start gap-2"
        [class]="'bg-' + color() + '-subtle'"
      >
        <div>
          <h2 class="h5 mb-0">{{ e.zonaAfectada }}</h2>
          <div class="small text-body-secondary">{{ e.municipio }} · Emergencia #{{ e.id }}</div>
        </div>
        <span class="badge" [class]="badge()">{{ e.nivelGravedadEtiqueta }}</span>
      </div>
      <div class="card-body d-flex flex-column gap-2">
        <p class="small text-body-secondary clamp-3 mb-1">{{ e.descripcion }}</p>
        <ng-content />
      </div>
      <div class="card-footer bg-body d-flex justify-content-between align-items-center gap-2">
        <span class="small text-body-secondary">
          Registrada: {{ e.fechaRegistro | date: fechaListado }}
        </span>
        <span class="badge" [class]="estadoLoteBadge()">{{ estadoLoteLabel() }}</span>
      </div>
    </a>
  `,
})
export class EmergenciaTarjeta {
  readonly emergencia = input.required<EmergenciaParaLoteResponse>();
  /** Comandos de `routerLink` a donde lleva la tarjeta. */
  readonly enlace = input.required<unknown[]>();

  protected readonly fechaListado = FECHA_LISTADO;

  protected readonly color = computed(
    () => NIVEL_GRAVEDAD_COLOR[this.emergencia().nivelGravedad] ?? 'secondary',
  );
  protected readonly badge = computed(
    () => NIVEL_GRAVEDAD_BADGE[this.emergencia().nivelGravedad] ?? 'text-bg-secondary',
  );
  protected readonly estadoLoteLabel = computed(() => {
    const estado = this.emergencia().estadoLote;
    return estado ? ESTADO_LOTE_LABEL[estado] : 'Sin lote';
  });
  protected readonly estadoLoteBadge = computed(() => {
    const estado = this.emergencia().estadoLote;
    return estado ? ESTADO_LOTE_BADGE[estado] : 'text-bg-secondary';
  });
}
