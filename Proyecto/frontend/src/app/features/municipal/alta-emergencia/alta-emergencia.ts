import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import {
  EmergenciaResponse,
  NIVELES_GRAVEDAD,
  NivelGravedad,
  NIVEL_GRAVEDAD_LABEL,
} from '../../../core/models/emergencia';

import { Emergencias } from '../../../core/services/emergencias';
import { mensajeDeError } from '../../../core/services/errores';
import { ToastService } from '../../../core/services/toast';

@Component({
  selector: 'app-alta-emergencia',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './alta-emergencia.html',
})
export class AltaEmergencia {
  private readonly fb = inject(FormBuilder);
  private readonly emergencias = inject(Emergencias);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  readonly nivelesGravedad = NIVELES_GRAVEDAD;
  readonly nivelGravedadLabel = NIVEL_GRAVEDAD_LABEL;

  cargando = signal<boolean>(false);

  form = this.fb.nonNullable.group({
    nivelGravedad: ['MEDIA' as NivelGravedad, [Validators.required]],
    zonaAfectada: ['', [Validators.required, Validators.maxLength(200)]],
    descripcion: [
      '',
      [Validators.required, Validators.minLength(10), Validators.maxLength(2000)],
    ],
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.cargando.set(true);

    this.emergencias.registrar(this.form.getRawValue()).subscribe({
      next: (res: EmergenciaResponse) => {
        this.cargando.set(false);
        this.toast.exito(
          'Emergencia registrada',
          `La emergencia #${res.id} se registró y se notificó al Centro Coordinador Regional.`,
        );
        // Al registrar se muestra el detalle de lo que se acaba de crear
        this.router.navigate(['/municipal/emergencias', res.id]);
      },

      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);

        this.toast.error(
          'No se pudo registrar la emergencia',
          mensajeDeError(err, 'Ocurrió un error al registrar la emergencia.'),
        );
      },
    });
  }
}
