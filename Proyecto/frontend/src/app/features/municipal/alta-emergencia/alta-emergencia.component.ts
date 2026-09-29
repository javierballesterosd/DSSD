import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { EmergenciaService } from '../../../core/services/emergencias/emergencia.service';
import {
  NIVELES_GRAVEDAD,
  NivelGravedad,
  EmergenciaResponse,
} from '../../../core/models/emergencia.model';

@Component({
  selector: 'app-alta-emergencia',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './alta-emergencia.component.html',
})
export class AltaEmergenciaComponent {
  private readonly fb = inject(FormBuilder);
  private readonly emergenciaService = inject(EmergenciaService);

  readonly nivelesGravedad = NIVELES_GRAVEDAD;

  cargando = signal<boolean>(false);
  mensajeExito = signal<string | null>(null);
  mensajeError = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    nivelGravedad: ['MEDIA' as NivelGravedad, [Validators.required]],
    zonaAfectada: ['', [Validators.required, Validators.maxLength(200)]],
    descripcion: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
  });

  /** Devuelve la descripción de la gravedad actualmente seleccionada */
  get descripcionGravedadSeleccionada(): string {
    const seleccion = this.form.controls.nivelGravedad.value;
    const item = this.nivelesGravedad.find((n) => n.clave === seleccion);
    return item ? item.descripcion : '';
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    this.mensajeExito.set(null);
    this.mensajeError.set(null);

    // El municipio lo resuelve el backend a partir del usuario logueado
    this.emergenciaService.registrarEmergencia(this.form.getRawValue()).subscribe({
      next: (res: EmergenciaResponse) => {
        this.cargando.set(false);
        this.mensajeExito.set('Emergencia registrada exitosamente');
        this.form.reset({
          nivelGravedad: 'MEDIA',
          zonaAfectada: '',
          descripcion: '',
        });
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        this.mensajeError.set(err.error?.message || 'Ocurrió un error al registrar la emergencia.');
      },
    });
  }
}
