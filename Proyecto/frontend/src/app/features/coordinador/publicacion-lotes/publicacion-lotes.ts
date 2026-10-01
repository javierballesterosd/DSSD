import { Component, computed, inject, OnInit, signal } from '@angular/core';

import {
  AbstractControl,
  ValidationErrors,
  ValidatorFn,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Emergencias } from '@core/services/emergencias';
import { Lotes } from '@core/services/lotes';
import { Recursos } from '@core/services/recursos';

import { Recurso } from '@core/models/recurso';
import { LoteRequest } from '@core/models/lote';

import { mensajeDeError } from '@core/services/errores';
import { ToastService } from '@core/services/toast';

import {
  EmergenciaParaLoteResponse,
  NIVEL_GRAVEDAD_BADGE,
  NIVEL_GRAVEDAD_COLOR,
} from '@core/models/emergencia';
import { aDatetimeLocal, proximaHoraEnPunto, sumarDias } from '@shared/formatos-fecha';

import { puedeDesglosar } from '../emergencias/emergencias-region';

interface ItemLoteForm {
  recursoId: FormControl<number>;
  cantidadRequerida: FormControl<number>;
}

type ItemLoteFormGroup = FormGroup<ItemLoteForm>;

/** Días entre la apertura y el cierre que propone el formulario. */
const DIAS_DE_CONVOCATORIA = 2;

// La apertura no puede quedar en el pasado (los datetime-local comparan bien como texto ISO)
const aperturaNoPasada: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  control.value && control.value < aDatetimeLocal(new Date()) ? { pasada: true } : null;

// El cierre debe ser posterior a la apertura (los datetime-local comparan bien como texto ISO)
const ventanaOfertasValida: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const apertura = group.get('fechaAperturaOfertas')?.value;
  const cierre = group.get('fechaCierreOfertas')?.value;
  return apertura && cierre && cierre <= apertura ? { ventanaInvalida: true } : null;
};

// Cada recurso va una sola vez en el lote (el select entrega el id como texto)
const recursosSinRepetir: ValidatorFn = (items: AbstractControl): ValidationErrors | null => {
  const ids = (items as FormArray<ItemLoteFormGroup>).controls
    .map((item) => Number(item.controls.recursoId.value))
    .filter((id) => id > 0);
  return new Set(ids).size < ids.length ? { recursosRepetidos: true } : null;
};

type PublicacionLotesForm = {
  titulo: FormControl<string>;
  fechaAperturaOfertas: FormControl<string>;
  fechaCierreOfertas: FormControl<string>;
  items: FormArray<ItemLoteFormGroup>;
};

/** Formulario para desglosar una emergencia en un lote de necesidades y publicarlo. */
@Component({
  selector: 'app-publicacion-lotes',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './publicacion-lotes.html',
})
export class PublicacionLotes implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly lotes = inject(Lotes);
  private readonly recursosService = inject(Recursos);
  private readonly emergenciasService = inject(Emergencias);
  private readonly toast = inject(ToastService);

  // ==========================================================
  // EMERGENCIA
  // ==========================================================

  emergencia = signal<EmergenciaParaLoteResponse | null>(null);
  cargandoEmergencia = signal<boolean>(true);
  errorEmergencia = signal<boolean>(false);

  /** false si la emergencia ya tiene un lote que no está cancelado. */
  readonly desglosable = computed(() => {
    const emergencia = this.emergencia();
    return !!emergencia && puedeDesglosar(emergencia);
  });
  readonly color = computed(() => {
    const nivel = this.emergencia()?.nivelGravedad;
    return (nivel && NIVEL_GRAVEDAD_COLOR[nivel]) ?? 'secondary';
  });
  readonly badge = computed(() => {
    const nivel = this.emergencia()?.nivelGravedad;
    return (nivel && NIVEL_GRAVEDAD_BADGE[nivel]) ?? 'text-bg-secondary';
  });

  // ==========================================================
  // RECURSOS
  // ==========================================================

  recursos = signal<Recurso[]>([]);
  cargandoRecursos = signal<boolean>(false);

  // ==========================================================
  // PUBLICACIÓN
  // ==========================================================

  cargando = signal<boolean>(false);

  /** Mínimo que acepta el selector de apertura: el momento en que se abrió el formulario. */
  readonly minimoApertura = aDatetimeLocal(new Date());

  // ==========================================================
  // FORMULARIO
  // ==========================================================

  form: FormGroup<PublicacionLotesForm> = this.fb.group(
    {
      titulo: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(150)]),
      fechaAperturaOfertas: this.fb.nonNullable.control('', [Validators.required, aperturaNoPasada]),
      fechaCierreOfertas: this.fb.nonNullable.control('', [Validators.required]),
      items: this.fb.array<ItemLoteFormGroup>([], recursosSinRepetir),
    },
    { validators: ventanaOfertasValida },
  );

  // ==========================================================
  // INIT
  // ==========================================================

  ngOnInit(): void {
    this.cargarEmergencia();
    this.cargarRecursos();
    this.proponerVentana();
    this.agregarItem();
  }

  private cargarEmergencia(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.emergenciasService.obtener(id).subscribe({
      next: (emergencia) => {
        this.emergencia.set(emergencia);
        this.cargandoEmergencia.set(false);
      },
      error: () => {
        this.errorEmergencia.set(true);
        this.cargandoEmergencia.set(false);
      },
    });
  }

  /** Por defecto la convocatoria abre a la próxima hora en punto y cierra a los dos días. */
  private proponerVentana(): void {
    const apertura = proximaHoraEnPunto();
    this.form.patchValue({
      fechaAperturaOfertas: aDatetimeLocal(apertura),
      fechaCierreOfertas: aDatetimeLocal(sumarDias(apertura, DIAS_DE_CONVOCATORIA)),
    });
  }

  // ==========================================================
  // RECURSOS
  // ==========================================================

  cargarRecursos(): void {
    this.cargandoRecursos.set(true);

    this.recursosService.listar().subscribe({
      next: (recursos) => {
        this.recursos.set(recursos);
        this.cargandoRecursos.set(false);
      },

      error: () => {
        this.cargandoRecursos.set(false);
        this.toast.error('No se pudieron cargar los recursos', 'Intentá de nuevo más tarde.');
      },
    });
  }

  // ==========================================================
  // ITEMS
  // ==========================================================

  get items(): FormArray<ItemLoteFormGroup> {
    return this.form.controls.items;
  }

  /** El recurso nuevo se agrega arriba; los que ya se completaron quedan debajo. */
  agregarItem(): void {
    const item: ItemLoteFormGroup = this.fb.group({
      recursoId: this.fb.nonNullable.control(0, [Validators.required, Validators.min(1)]),
      cantidadRequerida: this.fb.nonNullable.control(0, [Validators.required, Validators.min(1)]),
    });

    this.items.insert(0, item);
  }

  /** true si el recurso ya está elegido en otra fila (para no ofrecerlo de nuevo). */
  estaElegidoEnOtraFila(recursoId: number, fila: number): boolean {
    return this.items.controls.some(
      (item, i) => i !== fila && Number(item.controls.recursoId.value) === recursoId,
    );
  }

  /** No quedan recursos del catálogo sin usar en el lote. */
  sinRecursosDisponibles(): boolean {
    return this.recursos().length > 0 && this.items.length >= this.recursos().length;
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
    const emergencia = this.emergencia();

    if (!emergencia || !this.desglosable()) {
      this.toast.error(
        'No se pudo publicar el lote',
        'La emergencia seleccionada no permite crear un nuevo lote.',
      );
      return;
    }

    // La apertura pudo quedar en el pasado si el formulario estuvo abierto un rato
    this.form.controls.fechaAperturaOfertas.updateValueAndValidity();

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.cargando.set(true);

    const valores = this.form.getRawValue();

    const request: LoteRequest = {
      titulo: valores.titulo,
      fechaAperturaOfertas: valores.fechaAperturaOfertas,
      fechaCierreOfertas: valores.fechaCierreOfertas,
      items: valores.items.map((item) => ({
        recursoId: item.recursoId,
        cantidadRequerida: item.cantidadRequerida,
      })),
    };

    this.lotes.publicar(emergencia.id, request).subscribe({
      next: (res) => {
        this.cargando.set(false);
        this.toast.exito(
          'Lote publicado',
          `El lote «${res.titulo}» quedó publicado para la emergencia #${emergencia.id}.`,
        );
        // Al publicar se muestra el detalle de lo que se acaba de crear
        this.router.navigate(['/coordinador/lotes', res.id]);
      },

      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);

        this.toast.error(
          'No se pudo publicar el lote',
          mensajeDeError(err, 'Ocurrió un error al publicar el lote.'),
        );
      },
    });
  }
}
