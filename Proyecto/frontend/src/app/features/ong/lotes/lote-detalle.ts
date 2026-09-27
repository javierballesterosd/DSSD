import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NIVEL_GRAVEDAD_BADGE, NivelGravedad } from '../../../core/models/emergencia';
import { LoteDetalle as LoteDetalleModel } from '../../../core/models/lote';
import { OfertaResponse } from '../../../core/models/oferta';
import { OfertaFormModal } from '../ofertas/oferta-form-modal';
import { Lotes } from '../services/lotes';

@Component({
  imports: [RouterLink, DatePipe, OfertaFormModal],
  selector: 'app-lote-detalle',
  templateUrl: './lote-detalle.html',
})
export class LoteDetalle implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly lotesService = inject(Lotes);

  protected readonly nivelGravedadBadge = NIVEL_GRAVEDAD_BADGE;

  protected readonly lote = signal<LoteDetalleModel | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);
  protected readonly modalAbierto = signal(false);

  /** Evita recargar el lote (y con eso, desmontar el modal) mientras muestra el paso de éxito. */
  private huboOfertaRegistrada = false;

  ngOnInit(): void {
    this.cargarLote();
  }

  protected badgeGravedad(nivel: string): string {
    return this.nivelGravedadBadge[nivel as NivelGravedad] ?? 'text-bg-secondary';
  }

  protected abrirModal(): void {
    this.modalAbierto.set(true);
  }

  protected cerrarModal(): void {
    this.modalAbierto.set(false);
    if (this.huboOfertaRegistrada) {
      this.huboOfertaRegistrada = false;
      this.cargarLote();
    }
  }

  protected onOfertaRegistrada(_oferta: OfertaResponse): void {
    this.huboOfertaRegistrada = true;
  }

  private cargarLote(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.cargando.set(true);
    this.error.set(false);
    this.lotesService.obtenerDetalle(id).subscribe({
      next: (lote) => {
        this.lote.set(lote);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }
}
