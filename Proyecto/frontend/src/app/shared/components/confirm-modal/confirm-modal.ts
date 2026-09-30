import { Component, input, output } from '@angular/core';
import { Modal } from '../modal/modal';

/** Pregunta de confirmación genérica ("¿Seguro que…?") con Cancelar / Confirmar. */
@Component({
  imports: [Modal],
  selector: 'app-confirm-modal',
  template: `
    <app-modal [titulo]="titulo()" tamano="" (cerrar)="procesando() || cancelar.emit()">
      <p class="mb-0">{{ mensaje() }}</p>
      @if (detalle()) {
        <p class="small text-body-secondary mt-2 mb-0">{{ detalle() }}</p>
      }

      <div modal-footer>
        <button
          type="button"
          class="btn btn-secondary"
          [disabled]="procesando()"
          (click)="cancelar.emit()"
        >
          Cancelar
        </button>
        <button
          type="button"
          class="btn"
          [class]="'btn-' + variante()"
          [disabled]="procesando()"
          (click)="confirmar.emit()"
        >
          @if (procesando()) {
            <span class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>
          }
          {{ textoConfirmar() }}
        </button>
      </div>
    </app-modal>
  `,
})
export class ConfirmModal {
  readonly titulo = input.required<string>();
  readonly mensaje = input.required<string>();
  readonly detalle = input<string | null>(null);
  readonly textoConfirmar = input('Confirmar');
  readonly variante = input<'danger' | 'primary'>('danger');
  readonly procesando = input(false);
  readonly confirmar = output<void>();
  readonly cancelar = output<void>();
}
