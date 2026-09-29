import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  NIVEL_GRAVEDAD_BADGE,
  NIVEL_GRAVEDAD_COLOR,
  NivelGravedad,
} from '../../../core/models/emergencia';
import { LoteDetalle as LoteDetalleModel } from '../../../core/models/lote';
import { ESTADO_OFERTA_BADGE, OfertaResponse } from '../../../core/models/oferta';
import { Auth } from '../../../core/services/auth';
import { ToastService } from '../../../core/services/toast';
import { ConfirmModal } from '../../../shared/components/confirm-modal/confirm-modal';
import { FECHA_DIA, FECHA_LARGA, FECHA_LISTADO } from '../../../shared/formatos-fecha';
import { colorOng } from '../../../shared/ong-colores';
import { OfertaDetalleModal } from '../ofertas/oferta-detalle-modal';
import { OfertaFormModal } from '../ofertas/oferta-form-modal';
import { Lotes } from '../services/lotes';
import { Ofertas } from '../services/ofertas';

@Component({
  imports: [
    RouterLink,
    DatePipe,
    NgTemplateOutlet,
    OfertaFormModal,
    OfertaDetalleModal,
    ConfirmModal,
  ],
  selector: 'app-lote-detalle',
  templateUrl: './lote-detalle.html',
})
export class LoteDetalle implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly lotesService = inject(Lotes);
  private readonly ofertasService = inject(Ofertas);
  private readonly auth = inject(Auth);
  private readonly toast = inject(ToastService);

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
  /** Oferta abierta en el formulario en modo edición (null: el modal registra una nueva). */
  protected readonly ofertaEnEdicion = signal<OfertaResponse | null>(null);
  protected readonly ofertaAEliminar = signal<OfertaResponse | null>(null);
  protected readonly eliminando = signal(false);
  protected readonly colorOng = colorOng;
  protected readonly fechaLarga = FECHA_LARGA;
  protected readonly fechaDia = FECHA_DIA;
  protected readonly fechaListado = FECHA_LISTADO;

  /** Solo un representante de ONG puede ofertar (el backend responde 403 al resto). */
  protected readonly puedeOfertar = computed(() => this.auth.usuario()?.ongId != null);

  /** Evita recargar el lote (y con eso, desmontar el modal) mientras muestra el paso de éxito. */
  private huboCambios = false;

  /** ¿Ya ofertó la ONG del usuario para este lote? null mientras no se sepa (cargando, error o no es ONG). */
  protected readonly yaOferto = computed(() => {
    if (!this.puedeOfertar() || this.cargandoOfertas() || this.errorOfertas()) return null;
    return this.misOfertas().length > 0;
  });

  ngOnInit(): void {
    this.cargarLote();
    this.cargarMisOfertas();
  }

  protected colorGravedad(nivel: string): string {
    return NIVEL_GRAVEDAD_COLOR[nivel as NivelGravedad] ?? 'secondary';
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

  /** Editar/eliminar: solo ofertas pendientes y con la convocatoria del lote abierta (el backend lo revalida). */
  protected esModificable(oferta: OfertaResponse): boolean {
    return !!this.lote()?.convocatoriaAbierta && oferta.estado === 'PENDIENTE';
  }

  protected abrirModal(): void {
    this.ofertaEnEdicion.set(null);
    this.modalAbierto.set(true);
  }

  protected editar(oferta: OfertaResponse): void {
    this.ofertaEnEdicion.set(oferta);
    this.modalAbierto.set(true);
  }

  protected cerrarModal(): void {
    this.modalAbierto.set(false);
    this.ofertaEnEdicion.set(null);
    if (this.huboCambios) {
      this.huboCambios = false;
      this.cargarLote();
      this.cargarMisOfertas();
    }
  }

  protected onOfertaGuardada(_oferta: OfertaResponse): void {
    this.huboCambios = true;
  }

  protected confirmarEliminacion(): void {
    const oferta = this.ofertaAEliminar();
    if (!oferta || this.eliminando()) return;
    this.eliminando.set(true);
    this.ofertasService.eliminar(oferta.id).subscribe({
      next: () => {
        this.eliminando.set(false);
        this.ofertaAEliminar.set(null);
        this.toast.exito('Oferta eliminada', `La oferta #${oferta.id} se dio de baja.`);
        this.cargarMisOfertas();
      },
      error: (respuesta: HttpErrorResponse) => {
        this.eliminando.set(false);
        this.ofertaAEliminar.set(null);
        const mensaje = (respuesta.error as { mensaje?: string } | undefined)?.mensaje;
        this.toast.error(
          'No se pudo eliminar la oferta',
          mensaje ?? 'Ocurrió un error inesperado.',
        );
      },
    });
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
