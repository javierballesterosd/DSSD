import { Component, computed, output, signal } from '@angular/core';

export interface RangoFechas {
  desde?: string;
  hasta?: string;
}

@Component({
  selector: 'app-filtros-fecha',
  templateUrl: './filtros-fecha.html',
  styleUrl: './filtros-fecha.scss',
})
export class FiltrosFecha {
  readonly aplicar = output<RangoFechas>();
  readonly limpiar = output<void>();

  protected readonly desde = signal('');
  protected readonly hasta = signal('');

  protected readonly error = computed(() => {
    const desde = this.desde();
    const hasta = this.hasta();

    if (desde && hasta && desde > hasta) {
      return 'La fecha desde no puede ser posterior a la fecha hasta.';
    }

    return null;
  });

  protected cambiarDesde(event: Event): void {
    const valor = (event.target as HTMLInputElement).value;
    this.desde.set(valor);
  }

  protected cambiarHasta(event: Event): void {
    const valor = (event.target as HTMLInputElement).value;
    this.hasta.set(valor);
  }

  protected aplicarFiltros(): void {
    if (this.error()) {
      return;
    }

    this.aplicar.emit({
      desde: this.desde() || undefined,
      hasta: this.hasta() || undefined,
    });
  }

  protected limpiarFiltros(): void {
    this.desde.set('');
    this.hasta.set('');

    this.limpiar.emit();
  }
}
