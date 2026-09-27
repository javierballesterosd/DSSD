import { Component, inject, OnInit, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { LoteService } from '@core/services/lotes/lote.service';
import { RecursoService } from '@core/services/recursos/recurso.service';

import { Recurso } from '@core/models/recurso.model';
import { LoteResponse } from '@core/models/lote.model';

@Component({
  selector: 'app-publicacion-lotes',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './publicacion-lotes.component.html',
})
export class PublicacionLotesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly loteService = inject(LoteService);
  private readonly recursoService = inject(RecursoService);

  readonly emergenciaId = 1;

  recursos = signal<Recurso[]>([]);

  cargando = signal<boolean>(false);
  cargandoRecursos = signal<boolean>(false);

  mensajeExito = signal<string | null>(null);
  mensajeError = signal<string | null>(null);

  loteCreado = signal<LoteResponse | null>(null);

  form = this.fb.nonNullable.group({
    titulo: ['', [Validators.required, Validators.maxLength(150)]],

    fechaInicio: [''],

    items: this.fb.array([]),
  });

  ngOnInit(): void {
    this.cargarRecursos();
    this.agregarItem();
  }

  get items(): FormArray {
    return this.form.controls.items;
  }

  cargarRecursos(): void {
    this.cargandoRecursos.set(true);

    this.recursoService.obtenerRecursos().subscribe({
      next: (recursos) => {
        this.recursos.set(recursos);
        this.cargandoRecursos.set(false);
      },

      error: () => {
        this.cargandoRecursos.set(false);
        this.mensajeError.set('No se pudieron cargar los recursos disponibles.');
      },
    });
  }

  agregarItem(): void {
    const item = this.fb.nonNullable.group({
      recursoId: [0, [Validators.required, Validators.min(1)]],

      cantidadRequerida: [0, [Validators.required, Validators.min(1)]],
    });

    this.items.push(item);
  }

  eliminarItem(index: number): void {
    if (this.items.length === 1) {
      return;
    }

    this.items.removeAt(index);
  }

  recursoSeleccionado(index: number): Recurso | undefined {
    const recursoId = this.items.at(index).get('recursoId')?.value;

    return this.recursos().find((recurso) => recurso.id === recursoId);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    this.mensajeExito.set(null);
    this.mensajeError.set(null);

    const valores = this.form.getRawValue();

    const request = {
      titulo: valores.titulo,
      fechaInicio: valores.fechaInicio || undefined,
      items: valores.items.map((item) => ({
        recursoId: item.recursoId,
        cantidadRequerida: item.cantidadRequerida,
      })),
    };

    this.loteService.publicarLote(this.emergenciaId, request).subscribe({
      next: (res) => {
        this.cargando.set(false);
        this.loteCreado.set(res);
        this.mensajeExito.set('Lote publicado exitosamente.');

        this.form.reset({
          titulo: '',
          fechaInicio: '',
        });

        while (this.items.length > 0) {
          this.items.removeAt(0);
        }

        this.agregarItem();
      },

      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);

        this.mensajeError.set(err.error?.message || 'Ocurrió un error al publicar el lote.');
      },
    });
  }
}
