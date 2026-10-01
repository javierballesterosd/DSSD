import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, output, signal } from '@angular/core';
import { Notificaciones } from '@core/services/notificaciones';
import { Notificacion } from '@core/models/notificacion';
import { FECHA_LARGA } from '@shared/formatos-fecha';

@Component({
  imports: [DatePipe],
  selector: 'app-notifications',
  styleUrl: './notifications.scss',
  templateUrl: './notifications.html',
})
export class Notifications implements OnInit {
  protected readonly notificaciones = signal<Notificacion[]>([]);
  protected readonly pendientes = computed(() => this.notificaciones().length);
  protected readonly panelAbierto = signal(false);
  protected readonly fechaLarga = FECHA_LARGA;
  readonly panelCambiado = output<boolean>();
  private readonly notificacionesService = inject(Notificaciones);

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.notificacionesService.listar().subscribe({
      next: (notificaciones) => this.notificaciones.set(notificaciones),
      error: (error) => {
        console.error('No se pudieron cargar las notificaciones', error);
      },
    });
  }


  protected alternarPanel(): void {
    this.panelAbierto.update((abierto) => !abierto);
    this.panelCambiado.emit(this.panelAbierto());
  }

  cerrar(): void {
    this.panelAbierto.set(false);
  }

  protected eliminar(id: number): void {
    this.notificacionesService.eliminar(id).subscribe({
      next: () => {
        this.notificaciones.update((items) => items.filter((item) => item.id !== id));
      },
      error: (error) => {
        console.error('No se pudo eliminar la notificación', error);
      },
    });
  }
}
