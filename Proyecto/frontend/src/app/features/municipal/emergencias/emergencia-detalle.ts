import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  EmergenciaParaLoteResponse,
  NIVEL_GRAVEDAD_BADGE,
  NIVEL_GRAVEDAD_COLOR,
} from '../../../core/models/emergencia';
import { ESTADO_LOTE_BADGE, ESTADO_LOTE_LABEL } from '../../../core/models/lote';
import { Emergencias } from '../../../core/services/emergencias';
import { FECHA_LARGA } from '../../../shared/formatos-fecha';

@Component({
  imports: [RouterLink, DatePipe],
  selector: 'app-emergencia-detalle',
  templateUrl: './emergencia-detalle.html',
})
export class EmergenciaDetalle implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly emergenciasService = inject(Emergencias);

  protected readonly fechaLarga = FECHA_LARGA;

  protected readonly emergencia = signal<EmergenciaParaLoteResponse | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);

  protected readonly color = computed(() => {
    const nivel = this.emergencia()?.nivelGravedad;
    return (nivel && NIVEL_GRAVEDAD_COLOR[nivel]) ?? 'secondary';
  });
  protected readonly badge = computed(() => {
    const nivel = this.emergencia()?.nivelGravedad;
    return (nivel && NIVEL_GRAVEDAD_BADGE[nivel]) ?? 'text-bg-secondary';
  });
  protected readonly estadoLoteLabel = computed(() => {
    const estado = this.emergencia()?.estadoLote;
    return estado ? ESTADO_LOTE_LABEL[estado] : 'Sin lote';
  });
  protected readonly estadoLoteBadge = computed(() => {
    const estado = this.emergencia()?.estadoLote;
    return estado ? ESTADO_LOTE_BADGE[estado] : 'text-bg-secondary';
  });
  /** Qué está pasando con la emergencia del lado del Centro Coordinador. */
  protected readonly seguimiento = computed(() => {
    switch (this.emergencia()?.estadoLote) {
      case 'ACTIVO':
        return 'El Centro Coordinador Regional ya publicó el lote de necesidades de esta emergencia.';
      case 'FINALIZADO':
        return 'Las actividades del lote de esta emergencia ya finalizaron.';
      case 'CANCELADO':
        return 'El lote de esta emergencia fue cancelado; el Centro Coordinador Regional puede publicar uno nuevo.';
      default:
        return 'El Centro Coordinador Regional todavía no desglosó esta emergencia en un lote de necesidades.';
    }
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.emergenciasService.obtener(id).subscribe({
      next: (emergencia) => {
        this.emergencia.set(emergencia);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }
}
