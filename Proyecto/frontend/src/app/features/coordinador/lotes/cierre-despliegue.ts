import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { exhaustMap, Subscription, take, takeWhile, timer } from 'rxjs';
import { CierreDespliegueResponse } from '../../../core/models/emergencia';
import { Emergencias } from '../../../core/services/emergencias';
import { mensajeDeError } from '../../../core/services/errores';
import { ConfirmModal } from '../../../shared/components/confirm-modal/confirm-modal';

@Component({
  selector: 'app-cierre-despliegue',
  imports: [DatePipe, RouterLink, ReactiveFormsModule, ConfirmModal],
  templateUrl: './cierre-despliegue.html',
})
export class CierreDespliegue implements OnInit {
  private readonly api = inject(Emergencias);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private consulta?: Subscription;
  private readonly id = Number(this.route.snapshot.paramMap.get('id'));
  protected readonly cierre = signal<CierreDespliegueResponse | null>(null);
  protected readonly cargando = signal(true);
  protected readonly enviando = signal(false);
  protected readonly confirmando = signal(false);
  protected readonly error = signal('');
  protected readonly aviso = signal('');
  protected readonly form = new FormGroup({
    descripcionCierre: new FormControl('', { nonNullable: true, validators: [
      Validators.required, Validators.minLength(10), Validators.maxLength(2000),
      control => control.value?.trim().length >= 10 ? null : { textoCorto: true },
    ] }),
    confirmacion: new FormControl(false, { nonNullable: true, validators: [Validators.requiredTrue] }),
  });
  protected readonly habilitado = computed(() => {
    const c = this.cierre();
    return !!c && c.adjudicada && c.actividadesTotales > 0 && c.actividadesPendientes === 0 && !c.finalizada;
  });

  protected motivoBloqueo(): string {
    if (this.cargando()) return 'Esperá a que termine la consulta del estado.';
    if (this.enviando()) return 'La validación se está enviando.';
    const c = this.cierre();
    if (!c) return 'No se pudo consultar el estado. Actualizá para volver a intentarlo.';
    if (c.finalizada) return 'El despliegue ya está finalizado.';
    if (!c.adjudicada) return 'El municipio todavía no confirmó la aceptación de las ofertas en el Sistema Nacional.';
    if (c.actividadesTotales === 0) return 'La emergencia no tiene actividades registradas.';
    if (c.actividadesPendientes === 1) return 'Todavía queda 1 actividad pendiente de finalizar.';
    if (c.actividadesPendientes > 1) return `Todavía quedan ${c.actividadesPendientes} actividades pendientes de finalizar.`;
    if (this.form.controls.descripcionCierre.invalid) return 'Completá una descripción de cierre de entre 10 y 2000 caracteres.';
    if (!this.form.controls.confirmacion.value) return 'Marcá la confirmación de finalización de las actividades.';
    return '';
  }

  ngOnInit(): void { this.actualizar(); }

  protected actualizar(): void {
    this.error.set('');
    this.cargando.set(true);
    this.api.cierre(this.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: c => { this.recibir(c); this.cargando.set(false); },
      error: e => { this.error.set(mensajeDeError(e, 'No se pudo consultar el cierre.')); this.cargando.set(false); },
    });
  }

  private recibir(c: CierreDespliegueResponse): void {
    this.cierre.set(c);
    if (c.descripcionCierre != null) this.form.controls.descripcionCierre.setValue(c.descripcionCierre);
    if (c.finalizada) this.aviso.set('El despliegue finalizó y se liberaron los recursos.');
    else if (c.validacionRegistrada) this.aviso.set('La validación está registrada. El cierre queda pendiente hasta confirmar la liberación de recursos.');
  }

  protected solicitarConfirmacion(): void {
    this.form.markAllAsTouched();
    if (this.form.valid && this.habilitado() && !this.enviando()) this.confirmando.set(true);
  }

  protected validar(): void {
    if (!this.habilitado() || this.form.invalid || this.enviando()) return;
    this.error.set('');
    this.enviando.set(true);
    const texto = this.form.controls.descripcionCierre.value.trim();
    this.api.validarDespliegue(this.id, texto).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.enviando.set(false);
        this.confirmando.set(false);
        this.aviso.set('Validación enviada. Esperando la confirmación de liberación de recursos.');
        this.esperarCierre();
      },
      error: e => {
        this.enviando.set(false);
        this.confirmando.set(false);
        this.error.set(mensajeDeError(e, 'No se pudo validar el despliegue. Podés reintentar.'));
        // Una respuesta perdida puede dejar la validación guardada: releer sin borrar el texto ingresado.
        this.api.cierre(this.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
          next: c => this.recibir(c), error: () => {},
        });
      },
    });
  }

  private esperarCierre(): void {
    this.consulta?.unsubscribe();
    this.consulta = timer(0, 2500).pipe(
      take(24), exhaustMap(() => this.api.cierre(this.id)),
      takeWhile(c => !c.finalizada, true), takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: c => this.recibir(c),
      error: () => this.aviso.set('La validación fue enviada. Usá Actualizar estado para comprobar si el cierre terminó.'),
    });
  }
}
