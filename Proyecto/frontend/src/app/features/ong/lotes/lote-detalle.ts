import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NIVEL_GRAVEDAD_BADGE, NivelGravedad } from '../../../core/models/emergencia';
import { LoteDetalle as LoteDetalleModel } from '../../../core/models/lote';
import { ESTADO_OFERTA_BADGE, OfertaResponse } from '../../../core/models/oferta';
import { Auth } from '../../../core/services/auth';
import { colorOng } from '../../../shared/ong-colores';
import { OfertaDetalleModal } from '../ofertas/oferta-detalle-modal';
import { OfertaFormModal } from '../ofertas/oferta-form-modal';
import { Lotes } from '../services/lotes';
import { Ofertas } from '../services/ofertas';

@Component({
  imports: [RouterLink, DatePipe, OfertaFormModal, OfertaDetalleModal],
  selector: 'app-lote-detalle',
  templateUrl: './lote-detalle.html',
})
export class LoteDetalle implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly lotesService = inject(Lotes);
  private readonly ofertasService = inject(Ofertas);
  private readonly auth = inject(Auth);

  protected readonly nivelGravedadBadge = NIVEL_GRAVEDAD_BADGE;

  protected readonly lote = signal<LoteDetalleModel | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);
  protected readonly modalAbierto = signal(false);

  /** Ofertas de este lote en las que participa la ONG del usuario. */
  protected readonly misOfertas = signal<OfertaResponse[]>([]);
  protected readonly cargandoOfertas = signal(false);
  protected readonly errorOfertas = signal(false);
  protected readonly ofertaSeleccionada = signal<OfertaResponse | null>(null);
  protected readonly colorOng = colorOng;

  /** Solo un representante de ONG puede ofertar (el backend responde 403 al resto). */
  protected readonly puedeOfertar = computed(() => this.auth.usuario()?.ongId != null);

  /** Evita recargar el lote (y con eso, desmontar el modal) mientras muestra el paso de éxito. */
  private huboOfertaRegistrada = false;

  ngOnInit(): void {
    this.cargarLote();
    this.cargarMisOfertas();
  }

  protected badgeGravedad(nivel: string): string {
    return this.nivelGravedadBadge[nivel as NivelGravedad] ?? 'text-bg-secondary';
  }

  protected badgeEstado(estado: string): string {
    return ESTADO_OFERTA_BADGE[estado] ?? 'text-bg-secondary';
  }

  protected totalOfrecido(oferta: OfertaResponse): number {
    return oferta.aportes.reduce((suma, aporte) => suma + aporte.totalOfrecido, 0);
  }

  protected verDetalle(oferta: OfertaResponse): void {
    this.ofertaSeleccionada.set(oferta);
  }

  protected abrirModal(): void {
    this.modalAbierto.set(true);
  }

  protected cerrarModal(): void {
    this.modalAbierto.set(false);
    if (this.huboOfertaRegistrada) {
      this.huboOfertaRegistrada = false;
      this.cargarLote();
      this.cargarMisOfertas();
    }
  }

  protected onOfertaRegistrada(_oferta: OfertaResponse): void {
    this.huboOfertaRegistrada = true;
  }

  private cargarMisOfertas(): void {
    if (!this.puedeOfertar()) return;
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.cargandoOfertas.set(true);
    this.errorOfertas.set(false);
    this.ofertasService.misOfertas(id).subscribe({
      next: (ofertas) => {
        this.misOfertas.set(ofertas);
        this.cargandoOfertas.set(false);
      },
      error: () => {
        this.errorOfertas.set(true);
        this.cargandoOfertas.set(false);
      },
    });
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
