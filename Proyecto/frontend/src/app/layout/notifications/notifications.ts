import { Component, computed, output, signal } from '@angular/core';

@Component({
  selector: 'app-notifications',
  styleUrl: './notifications.scss',
  templateUrl: './notifications.html',
})
export class Notifications {
  protected readonly notificaciones = signal<NotificationItem[]>([]);
  protected readonly pendientes = computed(() => this.notificaciones().length);
  protected readonly panelAbierto = signal(false);
  readonly panelCambiado = output<boolean>();

  protected alternarPanel(): void {
    this.panelAbierto.update((abierto) => !abierto);
    this.panelCambiado.emit(this.panelAbierto());
  }

  cerrar(): void {
    this.panelAbierto.set(false);
  }

  protected eliminar(id: number): void {
    this.notificaciones.update((items) => items.filter((item) => item.id !== id));
  }
}

interface NotificationItem {
  id: number;
  title: string;
  description: string;
  createdAt?: string;
}
