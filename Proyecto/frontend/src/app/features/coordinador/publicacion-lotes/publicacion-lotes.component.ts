import { Component, inject, OnInit, signal } from '@angular/core';

import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { HttpErrorResponse } from '@angular/common/http';

import { LoteService } from '@core/services/lotes/lote.service';
import { RecursoService } from '@core/services/recursos/recurso.service';
import { EmergenciaService } from '../../municipal/services/emergencia';

import { Recurso } from '@core/models/recurso.model';
import { LoteRequest, LoteResponse } from '@core/models/lote.model';

import { EmergenciaLoteResponse } from '@core/models/emergencia-lote.model';

interface ItemLoteForm {
  recursoId: FormControl<number>;
  cantidadRequerida: FormControl<number>;
}

type ItemLoteFormGroup = FormGroup<ItemLoteForm>;

type PublicacionLotesForm = {
  titulo: FormControl<string>;
  fechaCierreOfertas: FormControl<string>;
  items: FormArray<ItemLoteFormGroup>;
};

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

  private readonly emergenciaService = inject(EmergenciaService);

  // ==========================================================
  // EMERGENCIAS
  // ==========================================================

  emergenciaSeleccionada: EmergenciaLoteResponse | null = null;

  emergencias = signal<EmergenciaLoteResponse[]>([]);

  cargandoEmergencias = signal<boolean>(false);

  errorEmergencias = signal<string>('');
  paginaActual = signal<number>(0);
  totalPaginas = signal<number>(0);
  totalEmergencias = signal<number>(0);

  readonly tamanioPagina = 10;

  // ==========================================================
  // RECURSOS
  // ==========================================================

  recursos = signal<Recurso[]>([]);

  cargandoRecursos = signal<boolean>(false);

  // ==========================================================
  // PUBLICACIÓN
  // ==========================================================

  cargando = signal<boolean>(false);

  mensajeExito = signal<string | null>(null);

  mensajeError = signal<string | null>(null);

  loteCreado = signal<LoteResponse | null>(null);

  // ==========================================================
  // FORMULARIO
  // ==========================================================

  form: FormGroup<PublicacionLotesForm> = this.fb.group({
    titulo: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(150)]),
    fechaCierreOfertas: this.fb.nonNullable.control('', [Validators.required]),
    items: this.fb.array<ItemLoteFormGroup>([]),
  });

  // ==========================================================
  // INIT
  // ==========================================================

  ngOnInit(): void {
    this.cargarEmergencias();

    this.cargarRecursos();

    this.agregarItem();
  }

  // ==========================================================
  // EMERGENCIAS
  // ==========================================================

  cargarEmergencias(pagina: number = this.paginaActual()): void {
    this.cargandoEmergencias.set(true);
    this.errorEmergencias.set('');

    this.emergenciaService.obtenerEmergenciasParaLotes(pagina, this.tamanioPagina).subscribe({
      next: (respuesta) => {
        this.emergencias.set(respuesta.content);

        this.paginaActual.set(respuesta.number);

        this.totalPaginas.set(respuesta.totalPages);

        this.totalEmergencias.set(respuesta.totalElements);

        this.cargandoEmergencias.set(false);
      },

      error: (error) => {
        console.error('Error al cargar emergencias:', error);

        this.emergencias.set([]);

        this.cargandoEmergencias.set(false);

        this.errorEmergencias.set('No se pudieron cargar las emergencias.');
      },
    });
  }

  paginaAnterior(): void {
    if (this.paginaActual() > 0) {
      this.cargarEmergencias(this.paginaActual() - 1);
    }
  }

  paginaSiguiente(): void {
    if (this.paginaActual() < this.totalPaginas() - 1) {
      this.cargarEmergencias(this.paginaActual() + 1);
    }
  }

  puedeDesglosar(emergencia: EmergenciaLoteResponse): boolean {
    return emergencia.estadoLote === null || emergencia.estadoLote === 'CANCELADO';
  }

  seleccionarEmergencia(emergencia: EmergenciaLoteResponse): void {
    if (!this.puedeDesglosar(emergencia)) {
      return;
    }

    this.emergenciaSeleccionada = emergencia;

    this.mensajeExito.set(null);

    this.mensajeError.set(null);

    this.loteCreado.set(null);

    this.form.reset({
      titulo: '',
      fechaCierreOfertas: '',
    });

    this.items.clear();

    this.agregarItem();
  }

  volverAemergencias(): void {
    this.emergenciaSeleccionada = null;

    this.mensajeExito.set(null);

    this.mensajeError.set(null);

    this.loteCreado.set(null);

    this.form.reset({
      titulo: '',
      fechaCierreOfertas: '',
    });

    this.items.clear();

    this.agregarItem();

    this.cargarEmergencias();
  }

  // ==========================================================
  // RECURSOS
  // ==========================================================

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

  // ==========================================================
  // ITEMS
  // ==========================================================

  get items(): FormArray<ItemLoteFormGroup> {
    return this.form.controls.items;
  }

  agregarItem(): void {
    const item: ItemLoteFormGroup = this.fb.group({
      recursoId: this.fb.nonNullable.control(0, [Validators.required, Validators.min(1)]),

      cantidadRequerida: this.fb.nonNullable.control(0, [Validators.required, Validators.min(1)]),
    });

    this.items.push(item);
  }

  eliminarItem(index: number): void {
    if (this.items.length === 1) {
      return;
    }

    this.items.removeAt(index);
  }

  // ==========================================================
  // SUBMIT
  // ==========================================================

  onSubmit(): void {
    if (!this.emergenciaSeleccionada) {
      this.mensajeError.set('Debe seleccionar una emergencia antes de publicar el lote.');

      return;
    }

    if (!this.puedeDesglosar(this.emergenciaSeleccionada)) {
      this.mensajeError.set('La emergencia seleccionada no permite crear un nuevo lote.');

      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();

      return;
    }

    this.cargando.set(true);

    this.mensajeExito.set(null);

    this.mensajeError.set(null);

    this.loteCreado.set(null);

    const valores = this.form.getRawValue();

    const request: LoteRequest = {
      titulo: valores.titulo,

      fechaCierreOfertas: valores.fechaCierreOfertas,

      items: valores.items.map((item) => ({
        recursoId: item.recursoId,

        cantidadRequerida: item.cantidadRequerida,
      })),
    };

    this.loteService.publicarLote(this.emergenciaSeleccionada.id, request).subscribe({
      next: (res) => {
        this.cargando.set(false);

        this.loteCreado.set(res);

        // ==================================================
        // MENSAJE
        // ==================================================

        this.mensajeExito.set('Lote publicado exitosamente.');

        // ==================================================
        // VOLVER AUTOMÁTICAMENTE A LA LISTA
        // ==================================================

        this.emergenciaSeleccionada = null;

        // ==================================================
        // LIMPIAR FORMULARIO
        // ==================================================

        this.form.reset({
          titulo: '',
          fechaCierreOfertas: '',
        });

        this.items.clear();

        this.agregarItem();

        // ==================================================
        // RECARGAR LISTA
        // ==================================================

        this.cargarEmergencias();
      },

      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);

        this.mensajeError.set(err.error?.message || 'Ocurrió un error al publicar el lote.');
      },
    });
  }
}
