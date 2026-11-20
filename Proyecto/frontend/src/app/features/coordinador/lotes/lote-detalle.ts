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
import { forkJoin } from 'rxjs';

interface ActividadSeguimiento {
  id: number;
  nombre: string;
  descripcion: string;
  estado: 'HECHA' | 'EN_CURSO' | 'PENDIENTE';
}

interface OngSeguimiento {
  id: number;
  nombre: string;
  actividades: ActividadSeguimiento[];
}

/** Detalle de un lote de la región del coordinador, con los datos de su emergencia. */
@Component({
  imports: [RouterLink, LoteFicha],
  selector: 'app-lote-detalle',
  templateUrl: './lote-detalle.html',
  styleUrl: './lote-detalle.css',
})
export class LoteDetalle implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly lotesService = inject(Lotes);
  private readonly emergenciasService = inject(Emergencias);

  protected readonly lote = signal<LoteDetalleModel | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);
  protected readonly seguimiento = signal<OngSeguimiento[]>([]);
  protected readonly actividadesTotales = signal(0);
  protected readonly expandida = signal<number | null>(null);
  protected readonly porcentajeGeneral = signal(0);
  protected readonly seguimientoError = signal(false);
  protected readonly cargandoSeguimiento = signal(true);

  protected porcentaje(ong: OngSeguimiento): number {
    return ong.actividades.length
      ? Math.round((ong.actividades.filter((a) => a.estado === 'HECHA').length / ong.actividades.length) * 100)
      : 0;
  }

  protected esHecha(actividad: ActividadSeguimiento): boolean {
    return actividad.estado === 'HECHA';
  }

  protected cantidadHechas(ong: OngSeguimiento): number {
    return ong.actividades.filter((actividad) => this.esHecha(actividad)).length;
  }

  protected toggle(ongId: number): void {
    this.expandida.update((actual) => actual === ongId ? null : ongId);
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
      ongs: this.lotesService.listarOngsAdjudicadas(loteId),
    }).subscribe({
      next: ({ emergencia, ongs }) => {
        const actividades = emergencia.actividades ?? [];
        this.actividadesTotales.set(actividades.length);
        const seguimiento = actividades.length ? this.armarSeguimiento(actividades, ongs) : [];
        this.seguimiento.set(seguimiento);
        this.porcentajeGeneral.set(seguimiento.length
          ? Math.round(seguimiento.reduce((total, ong) => total + this.porcentaje(ong), 0) / seguimiento.length)
          : 0);
        this.cargandoSeguimiento.set(false);
      },
      error: () => {
        this.seguimientoError.set(true);
        this.cargandoSeguimiento.set(false);
      },
    });
  }

  private armarSeguimiento(actividades: ActividadResponse[], ongs: { id: number; razonSocial: string }[]): OngSeguimiento[] {
    return ongs.map((ong, ongIndex) => {
      // Mientras no se persista el avance por ONG, simular porcentajes distintos para la vista CCR.
      const hechasSimuladas = Math.round((actividades.length * (ongIndex + 1)) / ongs.length);
      let hayActividadEnCurso = false;
      const progreso = actividades.map((actividad, actividadIndex): ActividadSeguimiento => {
        let estado: ActividadSeguimiento['estado'];
        if (actividad.finalizada || actividadIndex < hechasSimuladas) {
          estado = 'HECHA';
        } else if (!hayActividadEnCurso) {
          estado = 'EN_CURSO';
          hayActividadEnCurso = true;
        } else {
          estado = 'PENDIENTE';
        }
        return { id: actividad.id, nombre: actividad.nombre, descripcion: actividad.descripcion, estado };
      });
      return { id: ong.id, nombre: ong.razonSocial, actividades: progreso };
    });
  }
}
