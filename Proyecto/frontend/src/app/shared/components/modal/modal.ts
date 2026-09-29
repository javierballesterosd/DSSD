import { Component, DestroyRef, HostListener, inject, input, output } from '@angular/core';

@Component({
  selector: 'app-modal',
  templateUrl: './modal.html',
})
export class Modal {
  private readonly destroyRef = inject(DestroyRef);

  readonly titulo = input.required<string>();
  readonly tamano = input<'modal-sm' | 'modal-lg' | 'modal-xl' | ''>('modal-lg');
  readonly cerrar = output<void>();

  constructor() {
    document.body.classList.add('modal-open');
    this.destroyRef.onDestroy(() => document.body.classList.remove('modal-open'));
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.cerrar.emit();
  }

  protected onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cerrar.emit();
    }
  }
}
