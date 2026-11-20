import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  ESTADO_LOTE_BADGE,
  ESTADO_LOTE_LABEL,
  EstadoLote,
  LoteDetalle as LoteDetalleModel,
} from '../../../core/models/lote';
import type { ActividadResponse } from '@core/models/emergencia';
import { Emergencias } from '@core/services/emergencias';
import { Lotes } from '@core/services/lotes';
import { LoteFicha } from '@shared/components/lote-ficha/lote-ficha';
import { FECHA_LISTADO } from '@shared/formatos-fecha';
import { catchError, forkJoin, of } from 'rxjs';

/** Quién marcó la actividad como hecha (solo como control: el avance es común a todas las ONGs). */
interface MarcaDeOng {
  ong: string;
  fecha: string | null;
}

interface ActividadSeguimiento {
  id: number;
  nombre: string;
  descripcion: string;
  estado: 'HECHA' | 'PENDIENTE';
  /** Ofertas adjudicadas que ya la completaron, sobre el total que debe completarla. */
  ofertasFinalizadas: number;
  totalOfertas: number;
  marcas: MarcaDeOng[];
}

/** Detalle de un lote de la región del coordinador, con los datos de su emergencia. */
@Component({
  imports: [DatePipe, RouterLink, LoteFicha],
  selector: 'app-lote-detalle',
  templateUrl: './lote-detalle.html',
  styleUrl: './lote-detalle.css',
})
export class LoteDetalle implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly lotesService = inject(Lotes);
  private readonly emergenciasService = inject(Emergencias);

  protected readonly FECHA_LISTADO = FECHA_LISTADO;
  protected readonly lote = signal<LoteDetalleModel | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);
  protected readonly seguimiento = signal<ActividadSeguimiento[]>([]);
  /** ONGs de las ofertas adjudicadas (una oferta con más de una ONG es un consorcio). */
  protected readonly ongs = signal<{ id: number; razonSocial: string }[]>([]);
  protected readonly detalleAbierto = signal(false);
  protected readonly seguimientoError = signal(false);
  protected readonly cargandoSeguimiento = signal(true);

  protected readonly actividadesHechas = computed(
    () => this.seguimiento().filter((actividad) => actividad.estado === 'HECHA').length,
  );
  protected readonly porcentajeGeneral = computed(() => {
    const total = this.seguimiento().length;
    return total ? Math.round((this.actividadesHechas() / total) * 100) : 0;
  });
  /** Hay al menos una oferta adjudicada que debe completar las actividades. */
  protected readonly hayOfertasAdjudicadas = computed(() =>
    this.seguimiento().some((actividad) => actividad.totalOfertas > 0),
  );

  protected toggleDetalle(): void {
    this.detalleAbierto.update((abierto) => !abierto);
  }

  protected esParcial(actividad: ActividadSeguimiento): boolean {
    return actividad.estado === 'PENDIENTE' && actividad.ofertasFinalizadas > 0;
  }

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
        this.cargarActividades(lote.emergencia.id);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }

  private cargarActividades(emergenciaId: number): void {
    const loteId = this.lote()?.id;
    if (loteId == null) return;

    forkJoin({
      emergencia: this.emergenciasService.obtener(emergenciaId),
      // Si falla el listado de ONGs igual se muestra el avance de las actividades.
      ongs: this.lotesService.listarOngsAdjudicadas(loteId).pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ emergencia, ongs }) => {
        this.ongs.set(ongs);
        this.seguimiento.set((emergencia.actividades ?? []).map((a) => this.aSeguimiento(a)));
        this.cargandoSeguimiento.set(false);
      },
      error: () => {
        this.seguimientoError.set(true);
        this.cargandoSeguimiento.set(false);
      },
    });
  }

  /**
   * Las ONGs avanzan juntas: la actividad es una sola para toda la emergencia y está hecha cuando todas las
   * ofertas adjudicadas la completaron. Las marcas por ONG se muestran solo como control.
   */
  private aSeguimiento(actividad: ActividadResponse): ActividadSeguimiento {
    const avance = actividad.avance ?? [];
    const marcas = avance
      .filter((a) => a.finalizada && a.finalizadaPor)
      .map((a): MarcaDeOng => ({ ong: a.finalizadaPor!, fecha: a.fechaFinalizacion }));
    return {
      id: actividad.id,
      nombre: actividad.nombre,
      descripcion: actividad.descripcion,
      estado: actividad.finalizada ? 'HECHA' : 'PENDIENTE',
      ofertasFinalizadas: actividad.ofertasFinalizadas ?? 0,
      totalOfertas: actividad.totalOfertas ?? 0,
      marcas,
    };
  }
}
