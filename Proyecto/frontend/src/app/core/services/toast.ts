import { Service, signal } from '@angular/core';

export type ToastTipo = 'error' | 'exito' | 'info';

export interface Toast {
  id: number;
  tipo: ToastTipo;
  titulo: string;
  mensaje: string;
}

const DURACION_MS = 6000;

/** Avisos temporales globales. Se muestran en `ToastContainer` (montado en app.html). */
@Service()
export class ToastService {
  private siguienteId = 1;
  private readonly _toasts = signal<Toast[]>([]);
  readonly toasts = this._toasts.asReadonly();

  error(titulo: string, mensaje: string): void {
    this.mostrar('error', titulo, mensaje);
  }

  exito(titulo: string, mensaje: string): void {
    this.mostrar('exito', titulo, mensaje);
  }

  info(titulo: string, mensaje: string): void {
    this.mostrar('info', titulo, mensaje);
  }

  cerrar(id: number): void {
    this._toasts.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  private mostrar(tipo: ToastTipo, titulo: string, mensaje: string): void {
    const id = this.siguienteId++;
    this._toasts.update((toasts) => [...toasts, { id, tipo, titulo, mensaje }]);
    setTimeout(() => this.cerrar(id), DURACION_MS);
  }
}
