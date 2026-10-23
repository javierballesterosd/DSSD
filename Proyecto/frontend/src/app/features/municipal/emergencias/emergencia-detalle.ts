import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  EmergenciaParaLoteResponse,
  NIVEL_GRAVEDAD_BADGE,
  NIVEL_GRAVEDAD_COLOR,
  RecursosMunicipalesResponse,
} from '../../../core/models/emergencia';
import { ESTADO_LOTE_BADGE, ESTADO_LOTE_LABEL } from '../../../core/models/lote';
import { Emergencias } from '../../../core/services/emergencias';
import { ConfirmModal } from '../../../shared/components/confirm-modal/confirm-modal';
import { ToastService } from '../../../core/services/toast';
import { mensajeDeError } from '../../../core/services/errores';
import { FECHA_LARGA } from '../../../shared/formatos-fecha';

@Component({
  imports: [RouterLink, DatePipe, ConfirmModal],
  selector: 'app-emergencia-detalle',
  templateUrl: './emergencia-detalle.html',
})
export class EmergenciaDetalle implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly emergenciasService = inject(Emergencias);

  private readonly toast = inject(ToastService);
  protected readonly recursos = signal<RecursosMunicipalesResponse | null>(null);
  protected readonly errorRecursos = signal(false);
  protected readonly cargandoRecursos = signal(false);
  protected readonly confirmacion = signal<boolean | null>(null);
  protected readonly procesando = signal(false);

  protected cargarRecursos(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.errorRecursos.set(false);
    this.cargandoRecursos.set(true);
    this.emergenciasService.recursosAceptados(id).subscribe({
      next: (r) => {
        this.recursos.set(r);
        this.cargandoRecursos.set(false);
      },
      error: () => {
        this.errorRecursos.set(true);
        this.cargandoRecursos.set(false);
      },
    });
  }

  protected decidir(): void {
    const aceptar = this.confirmacion();
    const resumen = this.recursos();
    if (aceptar === null || resumen?.loteId == null || this.procesando()) return;
    this.procesando.set(true);
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.emergenciasService.decidir(id, resumen.loteId, aceptar).subscribe({
      next: () => {
        this.procesando.set(false);
        this.confirmacion.set(null);
        this.toast.exito(
          aceptar ? 'Recursos aceptados' : 'Ofertas rechazadas',
          aceptar
            ? 'Se confirmó el compromiso de todos los recursos habilitados.'
            : 'Se liberaron los recursos. Ahora debés actualizar la emergencia.',
        );
        this.ngOnInit();
      },
      error: (err) => {
        this.procesando.set(false);
        this.confirmacion.set(null);
        this.toast.error(
          'No se pudo completar la decisión',
          mensajeDeError(err, 'Intentá nuevamente.'),
        );
        this.cargarRecursos();
      },
    });
  }

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
    if (this.emergencia()?.requiereActualizacion) {
      return 'Rechazaste las ofertas. Debés actualizar la emergencia, aunque mantengas los mismos datos, para reiniciar el proceso.';
    }
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
    this.cargarRecursos();
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
