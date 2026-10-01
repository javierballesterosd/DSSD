import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import {
  EmergenciaResponse,
  NIVELES_GRAVEDAD,
  NivelGravedad,
  NIVEL_GRAVEDAD_LABEL
} from '../../../core/models/emergencia';

import { Emergencias } from '../../../core/services/emergencias';
import { mensajeDeError } from '../../../core/services/errores';
import { ToastService } from '../../../core/services/toast';

@Component({
  selector: 'app-alta-emergencia',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './alta-emergencia.html'
})
export class AltaEmergencia {
  private readonly fb = inject(FormBuilder);
  private readonly emergencias = inject(Emergencias);
  private readonly toast = inject(ToastService);

  readonly nivelesGravedad = NIVELES_GRAVEDAD;
  readonly nivelGravedadLabel = NIVEL_GRAVEDAD_LABEL;

  cargando = signal<boolean>(false);
  mensajeExito = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    nivelGravedad: ['MEDIA' as NivelGravedad, [Validators.required]],
    zonaAfectada: [
      '',
      [Validators.required, Validators.maxLength(200)]
    ],
    descripcion: [
      '',
      [
        Validators.required,
        Validators.minLength(10),
        Validators.maxLength(2000)
      ]
    ]
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    this.mensajeExito.set(null);

    this.emergencias
      .registrar(this.form.getRawValue())
      .subscribe({
        next: (res: EmergenciaResponse) => {
          this.cargando.set(false);

          this.mensajeExito.set(
            'Emergencia registrada exitosamente'
          );

          this.form.reset({
            nivelGravedad: 'MEDIA',
            zonaAfectada: '',
            descripcion: ''
          });
        },

        error: (err: HttpErrorResponse) => {
          this.cargando.set(false);

          this.toast.error(
            'No se pudo registrar la emergencia',
            mensajeDeError(err, 'Ocurrió un error al registrar la emergencia.')
          );
        }
      });
  }
}