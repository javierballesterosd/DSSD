import { DatePipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  NIVEL_GRAVEDAD_BADGE,
  NIVEL_GRAVEDAD_COLOR,
  NivelGravedad,
} from '../../../core/models/emergencia';
import { LoteResumen } from '../../../core/models/lote';
import { FECHA_DIA } from '../../formatos-fecha';

/**
 * Tarjeta de un lote para los listados; toda la tarjeta lleva a `enlace`. Cada perfil proyecta en el
 * pie su propio badge de estado.
 */
@Component({
  imports: [RouterLink, DatePipe],
  selector: 'app-lote-tarjeta',
  host: { class: 'd-block h-100' },
  template: `
    @let l = lote();
    <a
      class="card h-100 overflow-hidden shadow-sm text-decoration-none text-body tarjeta-hover"
      [routerLink]="enlace()"
    >
      <div
        class="card-header border-0 d-flex justify-content-between align-items-start gap-2"
        [class]="'bg-' + color() + '-subtle'"
      >
        <div>
          <h2 class="h5 mb-0">{{ l.titulo }}</h2>
          <div class="small text-body-secondary">
            {{ l.emergencia.zonaAfectada }} · {{ l.emergencia.municipio }}
          </div>
        </div>
        <span class="badge" [class]="badge()">{{ l.emergencia.nivelGravedadEtiqueta }}</span>
      </div>
      <div class="card-body d-flex flex-column gap-2">
        <p class="small text-body-secondary clamp-3 mb-1">{{ l.emergencia.descripcion }}</p>
        <div class="d-flex flex-wrap gap-1">
          @for (item of l.items; track item.id) {
            <span class="badge rounded-pill text-body bg-body-secondary border fw-normal">
              {{ item.recursoNombre }} · <strong>{{ item.cantidadRequerida }}</strong>
              {{ item.unidadMedida }}
            </span>
          }
        </div>
      </div>
      <div class="card-footer bg-body d-flex justify-content-between align-items-center gap-2">
        <span class="small text-body-secondary">
          Cierra:
          {{ l.fechaCierreOfertas ? (l.fechaCierreOfertas | date: fechaDia) : 'sin definir' }}
        </span>
        <ng-content />
      </div>
    </a>
  `,
})
export class LoteTarjeta {
  readonly lote = input.required<LoteResumen>();
  /** Comandos de `routerLink` del detalle del lote. */
  readonly enlace = input.required<unknown[]>();

  protected readonly fechaDia = FECHA_DIA;

  protected readonly color = computed(
    () => NIVEL_GRAVEDAD_COLOR[this.lote().emergencia.nivelGravedad as NivelGravedad] ?? 'secondary',
  );
  protected readonly badge = computed(
    () =>
      NIVEL_GRAVEDAD_BADGE[this.lote().emergencia.nivelGravedad as NivelGravedad] ??
      'text-bg-secondary',
  );
}
