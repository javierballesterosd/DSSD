import { DatePipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import {
  NIVEL_GRAVEDAD_BADGE,
  NIVEL_GRAVEDAD_COLOR,
  NivelGravedad,
} from '../../../core/models/emergencia';
import { LoteResumen } from '../../../core/models/lote';
import { FECHA_DIA, FECHA_LARGA } from '../../formatos-fecha';

/**
 * Ficha de un lote: encabezado con la gravedad de su emergencia, ventana de ofertas, descripción y
 * recursos solicitados. Cada perfil proyecta sus badges (`ficha-badges`) y acciones (`ficha-acciones`).
 */
@Component({
  imports: [DatePipe],
  selector: 'app-lote-ficha',
  template: `
    @let l = lote();
    <section class="rounded-4 px-4 py-3 mb-4" [class]="'bg-' + color() + '-subtle'">
      <div class="d-flex flex-wrap justify-content-between align-items-start gap-3">
        <div>
          <div class="d-flex flex-wrap gap-2">
            <span class="badge" [class]="badge()">{{ l.emergencia.nivelGravedadEtiqueta }}</span>
            <ng-content select="[ficha-badges]" />
          </div>
          <h1 class="h4 fw-semibold mt-2 mb-1">{{ l.titulo }}</h1>
          <div class="small text-body-secondary">
            {{ l.emergencia.zonaAfectada }} · {{ l.emergencia.municipio }}
          </div>
        </div>
        <ng-content select="[ficha-acciones]" />
      </div>
      <hr class="opacity-25 my-2" />
      <div class="row g-3">
        <div class="col-md-4">
          <div class="small text-uppercase text-body-secondary">Registrada</div>
          <div class="fw-semibold">{{ l.emergencia.fechaRegistro | date: fechaDia }}</div>
        </div>
        <div class="col-md-4">
          <div class="small text-uppercase text-body-secondary">Apertura de ofertas</div>
          <div class="fw-semibold">
            {{
              l.fechaAperturaOfertas
                ? (l.fechaAperturaOfertas | date: fechaLarga)
                : 'Sin definir aún'
            }}
          </div>
        </div>
        <div class="col-md-4">
          <div class="small text-uppercase text-body-secondary">Cierre de ofertas</div>
          <div class="fw-semibold">
            {{
              l.fechaCierreOfertas ? (l.fechaCierreOfertas | date: fechaLarga) : 'Sin definir aún'
            }}
          </div>
        </div>
      </div>
    </section>

    <div class="card border-0 shadow-sm mb-3">
      <div class="card-body py-3">
        <h2 class="h6 card-title">Descripción</h2>
        <p class="mb-0">{{ l.emergencia.descripcion }}</p>
      </div>
    </div>

    <div class="card border-0 shadow-sm mb-4">
      <div class="card-body py-3">
        <h2 class="h6 card-title mb-3">Recursos solicitados</h2>
        <div class="row row-cols-2 row-cols-md-3 row-cols-lg-6 g-2">
          @for (item of l.items; track item.id) {
            <div class="col">
              <div class="border rounded-3 px-3 py-2 h-100 bg-body-tertiary">
                <div class="small text-body-secondary">{{ item.recursoNombre }}</div>
                <div class="fw-semibold">{{ item.cantidadRequerida }} {{ item.unidadMedida }}</div>
              </div>
            </div>
          }
        </div>
      </div>
    </div>
  `,
})
export class LoteFicha {
  readonly lote = input.required<LoteResumen>();

  protected readonly fechaLarga = FECHA_LARGA;
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
