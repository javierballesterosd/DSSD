import { Component, computed, input } from '@angular/core';
import { AporteRecurso } from '../../../core/models/oferta';
import { colorOng } from '../../../shared/ong-colores';

/** Tabla de lo ofrecido: una fila por recurso, con el desglose por ONG y el total. */
@Component({
  selector: 'app-oferta-desglose',
  template: `
    <table class="table table-sm align-middle mb-0">
      <thead>
        <tr>
          <th>Recurso</th>
          <th>Desglose por ONG</th>
          <th>Total</th>
          @if (hayAceptadas()) {
            <th>Aceptado</th>
          }
          <th>Solicitado</th>
        </tr>
      </thead>
      <tbody>
        @for (aporte of aportes(); track aporte.itemLoteId) {
          <tr>
            <td>{{ aporte.recursoNombre }}</td>
            <td>
              @for (porOng of aporte.porOng; track porOng.ongId) {
                <div class="mb-1">
                  <span class="chip-ong" [style.--ong-color]="colorOng(porOng.ongId)">{{
                    porOng.razonSocial
                  }}</span>
                  {{ porOng.cantidadOfrecida }} {{ aporte.unidadMedida }}
                  @if (porOng.cantidadAceptada != null) {
                    <span class="text-body-secondary"
                      >· aceptado: <strong>{{ porOng.cantidadAceptada }}</strong></span
                    >
                  }
                </div>
              }
            </td>
            <td>{{ aporte.totalOfrecido }} {{ aporte.unidadMedida }}</td>
            @if (hayAceptadas()) {
              <td>{{ aporte.totalAceptado ?? 0 }} {{ aporte.unidadMedida }}</td>
            }
            <td>{{ aporte.cantidadRequerida }} {{ aporte.unidadMedida }}</td>
          </tr>
        }
      </tbody>
    </table>
  `,
})
export class OfertaDesglose {
  readonly aportes = input.required<AporteRecurso[]>();
  /** Solo se muestra lo aceptado una vez que el Sistema Nacional validó la oferta. */
  protected readonly hayAceptadas = computed(() => this.aportes().some((a) => a.totalAceptado != null));
  protected readonly colorOng = colorOng;
}
