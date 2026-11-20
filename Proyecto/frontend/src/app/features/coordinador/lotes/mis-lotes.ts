import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ESTADO_LOTE_BADGE, ESTADO_LOTE_LABEL, EstadoLote, LoteResumen } from '../../../core/models/lote';
import { Auth } from '../../../core/services/auth';
import { Lotes } from '../../../core/services/lotes';
import { LoteTarjeta } from '../../../shared/components/lote-tarjeta/lote-tarjeta';

/** Lotes publicados para las emergencias de la región del coordinador, en cualquier estado. */
@Component({
  imports: [RouterLink, LoteTarjeta],
  selector: 'app-mis-lotes',
  templateUrl: './mis-lotes.html',
})
export class MisLotes implements OnInit {
  private readonly lotesService = inject(Lotes);
  private readonly auth = inject(Auth);

  protected readonly region = this.auth.usuario()?.regionNombre ?? null;

  protected readonly pendientesValidacion = signal(false);
  private solicitudActual = 0;

  protected readonly lotes = signal<LoteResumen[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);

  ngOnInit(): void {
    const solicitud = ++this.solicitudActual;
    this.cargando.set(true);
    this.error.set(false);
    this.lotesService.listarMios(this.pendientesValidacion()).subscribe({
      next: (lotes) => {
        if (solicitud !== this.solicitudActual) return;
        this.lotes.set(lotes);
        this.cargando.set(false);
      },
      error: () => {
        if (solicitud !== this.solicitudActual) return;
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }

  protected cambiarFiltro(event: Event): void {
    this.pendientesValidacion.set((event.target as HTMLSelectElement).value === 'pendientes');
    this.ngOnInit();
  }

  protected estadoLabel(estado: string): string {
    return ESTADO_LOTE_LABEL[estado as EstadoLote] ?? estado;
  }

  protected estadoBadge(estado: string): string {
    return ESTADO_LOTE_BADGE[estado as EstadoLote] ?? 'text-bg-secondary';
  }
}
