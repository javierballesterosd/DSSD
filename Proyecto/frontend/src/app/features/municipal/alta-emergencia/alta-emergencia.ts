import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
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

  ngOnInit(): void {
    if (this.emergenciaId === null) return;
    this.cargandoDatos.set(true);
    this.emergencias.obtener(this.emergenciaId).subscribe({
      next: (e) => {
        this.cargandoDatos.set(false);
        if (!e.requiereActualizacion) {
          this.errorCarga.set(true);
          this.toast.error(
            'Actualización no disponible',
            'La emergencia no está pendiente de actualización por rechazo.',
          );
          return;
        }
        this.form.patchValue(e);
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

  readonly nivelesGravedad = NIVELES_GRAVEDAD;
  readonly nivelGravedadLabel = NIVEL_GRAVEDAD_LABEL;

  cargando = signal<boolean>(false);

  form = this.fb.nonNullable.group({
    nivelGravedad: ['MEDIA' as NivelGravedad, [Validators.required]],
    zonaAfectada: ['', [Validators.required, Validators.maxLength(200)]],
    descripcion: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
  });

  onSubmit(): void {
    if (this.cargando() || this.cargandoDatos() || this.errorCarga()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.cargando.set(true);

    const operacion =
      this.emergenciaId === null
        ? this.emergencias.registrar(this.form.getRawValue())
        : this.emergencias.actualizar(this.emergenciaId, this.form.getRawValue());
    operacion.subscribe({
      next: (res: EmergenciaResponse) => {
        this.cargando.set(false);
        this.toast.exito(
          this.emergenciaId === null ? 'Emergencia registrada' : 'Emergencia actualizada',
          `La emergencia #${res.id} se ${this.emergenciaId === null ? 'registró' : 'actualizó'} y se notificó al Centro Coordinador Regional.`,
        );
        // Al registrar se muestra el detalle de lo que se acaba de crear
        this.router.navigate(['/municipal/emergencias', res.id]);
      },

      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);

        this.toast.error(
          this.emergenciaId === null ? 'No se pudo registrar la emergencia' : 'No se pudo actualizar la emergencia',
          mensajeDeError(err, 'No se pudo guardar la emergencia. Intentá nuevamente.'),
        );
      },
    });
  }
}
