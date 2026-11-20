
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal, OnInit } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
  FormArray,
  FormGroup,
  FormControl
} from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';

import {
  EmergenciaResponse,
  NIVELES_GRAVEDAD,
  NivelGravedad,
  NIVEL_GRAVEDAD_LABEL,
} from '../../../core/models/emergencia';

import { Emergencias } from '../../../core/services/emergencias';
import { mensajeDeError } from '../../../core/services/errores';
import { ToastService } from '../../../core/services/toast';

// Estructura tipada de cada actividad
type ActividadForm = FormGroup<{
  nombre: FormControl<string>;
  descripcion: FormControl<string>;
}>;

@Component({
  selector: 'app-alta-emergencia',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './alta-emergencia.html',
})
export class AltaEmergencia implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly emergencias = inject(Emergencias);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly emergenciaId = Number(this.route.snapshot.paramMap.get('id')) || null;
  readonly cargandoDatos = signal(false);
  readonly errorCarga = signal(false);

  readonly nivelesGravedad = NIVELES_GRAVEDAD;
  readonly nivelGravedadLabel = NIVEL_GRAVEDAD_LABEL;

  cargando = signal<boolean>(false);

  form = this.fb.nonNullable.group({
    nivelGravedad: ['MEDIA' as NivelGravedad, [Validators.required]],
    zonaAfectada: ['', [Validators.required, Validators.maxLength(200)]],
    descripcion: [
      '',
      [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]
    ],
    // Toda emergencia necesita al menos una actividad
    actividades: this.fb.array<ActividadForm>([], [Validators.minLength(1)])
  });

  // Getter tipado para acceder al FormArray
  get actividades(): FormArray<ActividadForm> {
    return this.form.controls.actividades;
  }

  ngOnInit(): void {
    if (this.emergenciaId === null) {
      this.actividades.push(this.crearActividadForm());
      return;
    }

    this.cargandoDatos.set(true);

    this.emergencias.obtener(this.emergenciaId).subscribe({
      next: (e: any) => {
        this.cargandoDatos.set(false);

        if (!e.requiereActualizacion) {
          this.errorCarga.set(true);
          this.toast.error(
            'Actualización no disponible',
            'La emergencia no está pendiente de actualización por rechazo.',
          );
          return;
        }

        // Limpiar y poblar el FormArray con las actividades existentes
        this.actividades.clear();

        if (e.actividades && e.actividades.length > 0) {
          e.actividades.forEach((actividad: any) => {
            const actividadForm = this.crearActividadForm();
            actividadForm.patchValue({
              nombre: actividad.nombre,
              descripcion: actividad.descripcion ?? ''
            });
            this.actividades.push(actividadForm);
          });
        }

        // Emergencias viejas sin actividades: se pide cargar al menos una
        if (this.actividades.length === 0) this.actividades.push(this.crearActividadForm());

        this.form.patchValue({
          nivelGravedad: e.nivelGravedad,
          zonaAfectada: e.zonaAfectada,
          descripcion: e.descripcion
        });
      },
      error: (err) => {
        this.cargandoDatos.set(false);
        this.errorCarga.set(true);
        this.toast.error(
          'No se pudo cargar la emergencia',
          mensajeDeError(err, 'Intentá nuevamente.'),
        );
      },
    });
  }

  private crearActividadForm(): ActividadForm {
    return this.fb.nonNullable.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      descripcion: ['', [Validators.maxLength(500)]]
    });
  }

  agregarActividad(): void {
    this.actividades.push(this.crearActividadForm());
  }

  eliminarActividad(index: number): void {
    // La última actividad no se puede quitar
    if (this.actividades.length <= 1) return;
    this.actividades.removeAt(index);
  }

  onSubmit(): void {
    if (this.cargando() || this.cargandoDatos() || this.errorCarga()) return;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.cargando.set(true);

    const datos = this.form.getRawValue();

    const operacion =
      this.emergenciaId === null
        ? this.emergencias.registrar(datos)
        : this.emergencias.actualizar(this.emergenciaId, datos);

    operacion.subscribe({
      next: (res: EmergenciaResponse) => {
        this.cargando.set(false);
        this.toast.exito(
          this.emergenciaId === null ? 'Emergencia registrada' : 'Emergencia actualizada',
          `La emergencia «${res.zonaAfectada}» se ${this.emergenciaId === null ? 'registró' : 'actualizó'} y se notificó al Centro Coordinador Regional.`,
        );
        this.router.navigate(['/municipal/emergencias', res.id]);
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        this.toast.error(
          this.emergenciaId === null
            ? 'No se pudo registrar la emergencia'
            : 'No se pudo actualizar la emergencia',
          mensajeDeError(err, 'No se pudo guardar la emergencia. Intentá nuevamente.'),
        );
      },
    });
  }
}
