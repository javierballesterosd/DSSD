import {
  Component,
  computed,
  ElementRef,
  HostListener,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { Ong } from '../../../core/models/ong';
import { colorOng } from '../../../shared/ong-colores';

/** Minúsculas y sin acentos, para buscar "caritas" y encontrar "Cáritas". */
export function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
}

/**
 * Selección de las ONGs de una oferta: muestra solo las elegidas como chips y agrega nuevas
 * desde un desplegable con búsqueda. La ONG propia no se puede quitar.
 */
@Component({
  selector: 'app-ong-selector',
  template: `
    <div class="d-flex flex-wrap align-items-center gap-2">
      @for (ong of elegidas(); track ong.id) {
        <span
          class="chip-ong d-inline-flex align-items-center gap-1"
          [style.--ong-color]="colorOng(ong.id)"
        >
          {{ ong.razonSocial }}
          @if (ong.id === ongPropiaId()) {
            <span class="fw-normal">(tu ONG)</span>
          } @else {
            <button
              type="button"
              class="btn-close btn-close-sm ms-1"
              [attr.aria-label]="'Quitar ' + ong.razonSocial"
              (click)="quitar.emit(ong.id)"
            ></button>
          }
        </span>
      }

      <div class="position-relative">
        <button
          type="button"
          class="btn btn-outline-primary btn-sm"
          [disabled]="noElegidas().length === 0"
          (click)="alternar()"
        >
          + Agregar ONG
        </button>

        @if (abierto()) {
          <div class="dropdown-menu show p-2 selector-menu" (keydown.escape)="onEscape($event)">
            <input
              #busquedaInput
              type="search"
              class="form-control form-control-sm mb-2"
              placeholder="Buscar ONG…"
              aria-label="Buscar ONG"
              [value]="busqueda()"
              (input)="busqueda.set($any($event.target).value)"
              (keydown.enter)="elegirPrimera($event)"
            />
            <div class="selector-lista">
              @for (ong of filtradas(); track ong.id) {
                <button type="button" class="dropdown-item rounded" (click)="elegir(ong.id)">
                  <span class="chip-ong" [style.--ong-color]="colorOng(ong.id)">{{
                    ong.razonSocial
                  }}</span>
                </button>
              } @empty {
                <div class="dropdown-item-text small text-body-secondary">Sin resultados</div>
              }
            </div>
          </div>
        }
      </div>
    </div>
  `,
  styles: `
    .selector-menu {
      min-width: 20rem;
      z-index: 1070;
    }
    .selector-lista {
      max-height: 16rem;
      overflow-y: auto;
    }
    .btn-close-sm {
      width: 0.5em;
      height: 0.5em;
      padding: 0;
    }
  `,
})
export class OngSelector {
  private readonly elemento = inject(ElementRef<HTMLElement>);

  readonly ongs = input.required<Ong[]>();
  readonly seleccionadas = input.required<number[]>();
  readonly ongPropiaId = input<number | null>(null);
  readonly agregar = output<number>();
  readonly quitar = output<number>();

  protected readonly abierto = signal(false);
  protected readonly busqueda = signal('');
  private readonly busquedaInput = viewChild<ElementRef<HTMLInputElement>>('busquedaInput');

  protected readonly colorOng = colorOng;

  /** Las elegidas, con la propia primero. */
  protected readonly elegidas = computed(() => {
    const propia = this.ongPropiaId();
    return this.ongs()
      .filter((ong) => this.seleccionadas().includes(ong.id))
      .sort((a, b) => {
        if (a.id === propia) return -1;
        if (b.id === propia) return 1;
        return a.razonSocial.localeCompare(b.razonSocial);
      });
  });

  protected readonly noElegidas = computed(() =>
    this.ongs()
      .filter((ong) => !this.seleccionadas().includes(ong.id))
      .sort((a, b) => a.razonSocial.localeCompare(b.razonSocial)),
  );

  protected readonly filtradas = computed(() => {
    const texto = normalizar(this.busqueda());
    if (!texto) return this.noElegidas();
    return this.noElegidas().filter((ong) => normalizar(ong.razonSocial).includes(texto));
  });

  protected alternar(): void {
    if (this.abierto()) {
      this.cerrarMenu();
      return;
    }
    this.busqueda.set('');
    this.abierto.set(true);
    setTimeout(() => this.busquedaInput()?.nativeElement.focus());
  }

  protected elegir(ongId: number): void {
    this.agregar.emit(ongId);
    this.cerrarMenu();
  }

  protected elegirPrimera(event: Event): void {
    event.preventDefault();
    const primera = this.filtradas()[0];
    if (primera) this.elegir(primera.id);
  }

  protected onEscape(event: Event): void {
    // Que el Escape cierre el desplegable y no llegue al modal que lo contiene (escucha en document).
    event.stopPropagation();
    this.cerrarMenu();
  }

  @HostListener('document:click', ['$event'])
  protected onClickAfuera(event: MouseEvent): void {
    if (this.abierto() && !this.elemento.nativeElement.contains(event.target as Node)) {
      this.cerrarMenu();
    }
  }

  private cerrarMenu(): void {
    this.abierto.set(false);
    this.busqueda.set('');
  }
}
