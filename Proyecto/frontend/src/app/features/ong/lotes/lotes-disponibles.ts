import { Component, inject, OnInit, signal } from '@angular/core';
import { LoteResumen } from '../../../core/models/lote';
import { Auth } from '../../../core/services/auth';
import { Lotes } from '../../../core/services/lotes';
import { LoteTarjeta } from '../../../shared/components/lote-tarjeta/lote-tarjeta';
import { Ofertas } from '../services/ofertas';

@Component({
  imports: [LoteTarjeta],
  selector: 'app-lotes-disponibles',
  templateUrl: './lotes-disponibles.html',
})
export class LotesDisponibles implements OnInit {
  private readonly lotesService = inject(Lotes);
  private readonly ofertasService = inject(Ofertas);
  private readonly auth = inject(Auth);

  protected readonly lotes = signal<LoteResumen[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);
  /** Ids de los lotes donde la ONG ya ofertó; null si no se sabe (no es ONG, cargando o error). */
  protected readonly lotesConOferta = signal<Set<number> | null>(null);

  ngOnInit(): void {
    if (this.auth.usuario()?.ongId != null) {
      this.ofertasService.lotesConMisOfertas().subscribe({
        next: (ids) => this.lotesConOferta.set(new Set(ids)),
        error: () => undefined,
      });
    }
    this.lotesService.listarPublicados().subscribe({
      next: (lotes) => {
        this.lotes.set(lotes);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }

  /** La ventana de ofertas del lote todavía no abrió. */
  protected noInicio(lote: LoteResumen): boolean {
    return !!lote.fechaAperturaOfertas && new Date(lote.fechaAperturaOfertas) > new Date();
  }

  /** true/false si se sabe si la ONG ofertó en el lote; null si no corresponde mostrarlo. */
  protected yaOferto(loteId: number): boolean | null {
    return this.lotesConOferta()?.has(loteId) ?? null;
  }
}
