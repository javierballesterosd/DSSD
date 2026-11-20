import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActividadDespliegue, MiDespliegue } from '../../../core/models/despliegue';
import { ESTADO_OFERTA_BADGE } from '../../../core/models/oferta';
import { mensajeDeError } from '../../../core/services/errores';
import { ToastService } from '../../../core/services/toast';
import { ConfirmModal } from '../../../shared/components/confirm-modal/confirm-modal';
import { FECHA_LISTADO } from '../../../shared/formatos-fecha';
import { Despliegues } from '../services/despliegues';

/** Una actividad que la ONG está por marcar como hecha (pide confirmación antes). */
interface Pendiente {
  despliegue: MiDespliegue;
  actividad: ActividadDespliegue;
}

@Component({
  imports: [ConfirmModal, DatePipe],
  selector: 'app-mis-despliegues',
  templateUrl: './mis-despliegues.html',
})
export class MisDespliegues implements OnInit {
  private readonly despliegues = inject(Despliegues);
  private readonly toast = inject(ToastService);

  protected readonly FECHA_LISTADO = FECHA_LISTADO;
  protected readonly lista = signal<MiDespliegue[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);
  protected readonly pendiente = signal<Pendiente | null>(null);
  protected readonly procesando = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  protected badgeEstado(estado: string): string {
    return ESTADO_OFERTA_BADGE[estado] ?? 'text-bg-secondary';
  }

  protected porcentaje(d: MiDespliegue): number {
    return d.totalActividades === 0
      ? 0
      : Math.round((d.actividadesFinalizadas / d.totalActividades) * 100);
  }

  protected esConsorcio(d: MiDespliegue): boolean {
    return d.ongs.length > 1;
  }

  protected pedirConfirmacion(despliegue: MiDespliegue, actividad: ActividadDespliegue): void {
    this.pendiente.set({ despliegue, actividad });
  }

  protected cancelar(): void {
    if (!this.procesando()) {
      this.pendiente.set(null);
    }
  }

  protected confirmar(): void {
    const seleccion = this.pendiente();
    if (!seleccion || this.procesando()) {
      return;
    }
    this.procesando.set(true);
    this.despliegues
      .finalizarActividad(seleccion.despliegue.ofertaId, seleccion.actividad.actividadId)
      .subscribe({
        next: () => {
          this.procesando.set(false);
          this.pendiente.set(null);
          this.toast.exito(
            'Actividad finalizada',
            `Marcaste «${seleccion.actividad.nombre}» como hecha.`,
          );
          this.cargar(false);
        },
        error: (error) => {
          this.procesando.set(false);
          this.pendiente.set(null);
          this.toast.error(
            'No se pudo finalizar la actividad',
            mensajeDeError(error, 'Probá de nuevo más tarde.'),
          );
          this.cargar(false);
        },
      });
  }

  /** Recarga el listado; tras una acción no vuelve a mostrar "Cargando…" para no parpadear. */
  private cargar(mostrarCarga = true): void {
    if (mostrarCarga) {
      this.cargando.set(true);
    }
    this.despliegues.misDespliegues().subscribe({
      next: (lista) => {
        this.lista.set(lista);
        this.error.set(false);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }
}
