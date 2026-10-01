import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  ESTADO_LOTE_BADGE,
  ESTADO_LOTE_LABEL,
  EstadoLote,
  LoteDetalle as LoteDetalleModel,
} from '../../../core/models/lote';
import { Lotes } from '../../../core/services/lotes';
import { LoteFicha } from '../../../shared/components/lote-ficha/lote-ficha';

/** Detalle de un lote de la región del coordinador, con los datos de su emergencia. */
@Component({
  imports: [RouterLink, LoteFicha],
  selector: 'app-lote-detalle',
  templateUrl: './lote-detalle.html',
})
export class LoteDetalle implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly lotesService = inject(Lotes);

  protected readonly lote = signal<LoteDetalleModel | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);

  protected readonly estadoLabel = computed(() => {
    const estado = this.lote()?.estado;
    return (estado && ESTADO_LOTE_LABEL[estado as EstadoLote]) ?? estado ?? '';
  });
  protected readonly estadoBadge = computed(
    () => ESTADO_LOTE_BADGE[this.lote()?.estado as EstadoLote] ?? 'text-bg-secondary',
  );
  /** En qué punto está la recepción de ofertas del lote. */
  protected readonly convocatoria = computed(() => {
    const lote = this.lote();
    if (!lote || lote.estado !== 'ACTIVO') return null;
    if (lote.convocatoriaAbierta) return 'Convocatoria abierta';
    const apertura = lote.fechaAperturaOfertas;
    return apertura && new Date(apertura) > new Date()
      ? 'Convocatoria todavía no iniciada'
      : 'Convocatoria cerrada';
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
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
