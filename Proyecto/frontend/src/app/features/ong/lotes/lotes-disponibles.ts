import { DatePipe, SlicePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NIVEL_GRAVEDAD_BADGE, NivelGravedad } from '../../../core/models/emergencia';
import { LoteResumen } from '../../../core/models/lote';
import { Lotes } from '../services/lotes';

@Component({
  imports: [RouterLink, DatePipe, SlicePipe],
  selector: 'app-lotes-disponibles',
  templateUrl: './lotes-disponibles.html',
})
export class LotesDisponibles implements OnInit {
  private readonly lotesService = inject(Lotes);

  protected readonly nivelGravedadBadge = NIVEL_GRAVEDAD_BADGE;

  protected readonly lotes = signal<LoteResumen[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);

  ngOnInit(): void {
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

  protected badgeGravedad(nivel: string): string {
    return this.nivelGravedadBadge[nivel as NivelGravedad] ?? 'text-bg-secondary';
  }
}
