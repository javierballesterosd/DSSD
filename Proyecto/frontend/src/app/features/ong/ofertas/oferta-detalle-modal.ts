import { DatePipe } from '@angular/common';
import { Component, input, output, signal } from '@angular/core';
import { ESTADO_OFERTA_BADGE, OfertaResponse } from '../../../core/models/oferta';
import { Modal } from '../../../shared/components/modal/modal';
import { FECHA_LARGA } from '../../../shared/formatos-fecha';
import { colorOng } from '../../../shared/ong-colores';
import { OfertaDesglose } from './oferta-desglose';
import { OfertaHistorial } from './oferta-historial';

@Component({
  imports: [Modal, OfertaDesglose, OfertaHistorial, DatePipe],
  selector: 'app-oferta-detalle-modal',
  template: `
    @let o = oferta();
    <app-modal [titulo]="'Oferta #' + o.id" tamano="modal-lg" (cerrar)="cerrar.emit()">
      <ul class="nav nav-tabs mb-3">
        <li class="nav-item">
          <button
            type="button"
            class="nav-link"
            [class.active]="pestana() === 'detalle'"
            (click)="pestana.set('detalle')"
          >
            Detalle
          </button>
        </li>
        <li class="nav-item">
          <button
            type="button"
            class="nav-link"
            [class.active]="pestana() === 'historial'"
            (click)="pestana.set('historial')"
          >
            Historial
          </button>
        </li>
      </ul>

      @if (pestana() === 'historial') {
        <app-oferta-historial [ofertaId]="o.id" />
      } @else {
        <dl class="row mb-3">
          <dt class="col-sm-3">Lote</dt>
          <dd class="col-sm-9">{{ o.loteTitulo }} · {{ o.emergenciaZona }}</dd>

          <dt class="col-sm-3">Estado</dt>
          <dd class="col-sm-9">
            <span class="badge" [class]="badge(o.estado)">{{ o.estadoEtiqueta }}</span>
          </dd>

          <dt class="col-sm-3">Registrada</dt>
          <dd class="col-sm-9">{{ o.fechaOferta | date: fechaLarga }}</dd>

          @if (o.fechaModificacion) {
            <dt class="col-sm-3">Modificada</dt>
            <dd class="col-sm-9">{{ o.fechaModificacion | date: fechaLarga }}</dd>
          }

          <dt class="col-sm-3">ONGs</dt>
          <dd class="col-sm-9 mb-0 d-flex flex-wrap gap-2">
            @for (ong of o.ongs; track ong.id) {
              <span class="chip-ong" [style.--ong-color]="colorOng(ong.id)">{{
                ong.razonSocial
              }}</span>
            }
          </dd>
        </dl>

        <app-oferta-desglose [aportes]="o.aportes" />
      }

      <div modal-footer>
        <button type="button" class="btn btn-primary" (click)="cerrar.emit()">Cerrar</button>
      </div>
    </app-modal>
  `,
})
export class OfertaDetalleModal {
  readonly oferta = input.required<OfertaResponse>();
  readonly cerrar = output<void>();

  protected readonly pestana = signal<'detalle' | 'historial'>('detalle');

  protected readonly colorOng = colorOng;
  protected readonly fechaLarga = FECHA_LARGA;

  protected badge(estado: string): string {
    return ESTADO_OFERTA_BADGE[estado] ?? 'text-bg-secondary';
  }
}
