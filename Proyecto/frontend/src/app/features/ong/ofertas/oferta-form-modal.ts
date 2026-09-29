import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LoteDetalle } from '../../../core/models/lote';
import { Observable } from 'rxjs';
import { DetalleOfertaRequest, OfertaResponse } from '../../../core/models/oferta';
import { InventarioOng, Ong } from '../../../core/models/ong';
import { Auth } from '../../../core/services/auth';
import { Modal } from '../../../shared/components/modal/modal';
import { colorOng } from '../../../shared/ong-colores';
import { Ofertas } from '../services/ofertas';
import { Ongs } from '../services/ongs';
import { OfertaDesglose } from './oferta-desglose';
import { OngSelector } from './ong-selector';
import {
  estadoCelda,
  excedeSolicitado,
  indexarInventario,
  ongsSinAporte,
  sugerencias,
  totalFila,
} from './disponibilidad';

interface ErrorBackend {
  mensaje?: string;
  detalles?: string[];
}

@Component({
  imports: [Modal, FormsModule, OfertaDesglose, OngSelector],
  selector: 'app-oferta-form-modal',
  templateUrl: './oferta-form-modal.html',
  styleUrl: './oferta-form-modal.scss',
})
export class OfertaFormModal implements OnInit {
  private readonly ongsService = inject(Ongs);
  private readonly ofertasService = inject(Ofertas);
  private readonly auth = inject(Auth);

  readonly lote = input.required<LoteDetalle>();
  /** Si viene, el modal edita esa oferta (solo cantidades) en vez de registrar una nueva. */
  readonly oferta = input<OfertaResponse | null>(null);
  readonly guardada = output<OfertaResponse>();
  readonly cerrar = output<void>();

  protected readonly paso = signal<'formulario' | 'exito'>('formulario');
  protected readonly ongsDisponibles = signal<Ong[]>([]);
  protected readonly ongsSeleccionadas = signal<number[]>([]);
  protected readonly inventario = signal<InventarioOng[]>([]);
  protected readonly cantidades = signal<Map<string, number | null>>(new Map());
  protected readonly enviando = signal(false);
  protected readonly errorMensaje = signal<string | null>(null);
  protected readonly errorDetalles = signal<string[]>([]);
  protected readonly ofertaGuardada = signal<OfertaResponse | null>(null);

  protected readonly modoEdicion = computed(() => this.oferta() !== null);
  protected readonly titulo = computed(() => {
    const oferta = this.oferta();
    return oferta ? `Editar oferta #${oferta.id}` : 'Registrar oferta';
  });

  /** ONG del usuario logueado: participa siempre de la oferta y no se puede desmarcar. */
  protected readonly ongPropiaId = computed(() => this.auth.usuario()?.ongId ?? null);

  /** La ONG propia primero; el resto por razón social. */
  protected readonly ongsOrdenadas = computed(() => {
    const propia = this.ongPropiaId();
    return [...this.ongsDisponibles()].sort((a, b) => {
      if (a.id === propia) return -1;
      if (b.id === propia) return 1;
      return a.razonSocial.localeCompare(b.razonSocial);
    });
  });

  protected readonly inventarioIndice = computed(() => indexarInventario(this.inventario()));

  protected readonly columnas = computed(() =>
    this.ongsOrdenadas().filter((ong) => this.ongsSeleccionadas().includes(ong.id)),
  );

  protected readonly ongsSinAportar = computed(() => {
    const valores = new Map<number, number[]>();
    for (const ongId of this.ongsSeleccionadas()) {
      const cantidadesOng = this.lote().items.map((item) => this.cantidad(item.id, ongId) ?? 0);
      valores.set(ongId, cantidadesOng);
    }
    return ongsSinAporte(this.ongsSeleccionadas(), valores);
  });

  protected readonly hayCeldaInvalida = computed(() => {
    for (const item of this.lote().items) {
      for (const ong of this.columnas()) {
        const estado = this.estado(item.id, ong.id);
        if (estado === 'excede-inventario' || estado === 'invalida') {
          return true;
        }
      }
    }
    return false;
  });

  protected readonly hayAlgunAporte = computed(() => {
    for (const [, cantidad] of this.cantidades()) {
      if (cantidad && cantidad > 0) {
        return true;
      }
    }
    return false;
  });

  protected readonly motivos = computed(() => {
    const motivos: string[] = [];
    if (this.ongsSeleccionadas().length === 0) {
      motivos.push('Elegí al menos una ONG para empezar a cargar.');
    }
    if (this.ongsSeleccionadas().length > 0 && !this.hayAlgunAporte()) {
      motivos.push('Cargá al menos una cantidad mayor a cero.');
    }
    if (this.hayCeldaInvalida()) {
      motivos.push('Hay celdas que superan el inventario disponible o tienen un valor inválido.');
    }
    for (const ongId of this.ongsSinAportar()) {
      if (ongId === this.ongPropiaId()) {
        motivos.push('Tu ONG todavía no aporta nada: cargá al menos una cantidad.');
        continue;
      }
      const ong = this.ongsDisponibles().find((o) => o.id === ongId);
      motivos.push(
        `«${ong?.razonSocial}» no está aportando nada: cargale una cantidad o sacala de la selección.`,
      );
    }
    return motivos;
  });

  protected readonly formularioValido = computed(() => this.motivos().length === 0);

  ngOnInit(): void {
    const oferta = this.oferta();
    if (oferta) {
      this.precargar(oferta);
      return;
    }

    this.ongsService.listar().subscribe((ongs) => this.ongsDisponibles.set(ongs));

    const propia = this.ongPropiaId();
    if (propia !== null) {
      this.ongsSeleccionadas.set([propia]);
      this.refrescarInventario([propia]);
    }
  }

  protected esOngPropia(ongId: number): boolean {
    return ongId === this.ongPropiaId();
  }

  protected toggleOng(ongId: number, marcado: boolean): void {
    if (this.esOngPropia(ongId) || this.modoEdicion()) return;
    const actuales = this.ongsSeleccionadas();
    const nuevas = marcado ? [...actuales, ongId] : actuales.filter((id) => id !== ongId);
    this.ongsSeleccionadas.set(nuevas);
    this.refrescarInventario(nuevas);
  }

  protected cantidad(itemLoteId: number, ongId: number): number | null {
    return this.cantidades().get(this.clave(itemLoteId, ongId)) ?? null;
  }

  protected setCantidad(itemLoteId: number, ongId: number, valor: string): void {
    const numero = valor === '' ? null : Number(valor);
    const mapa = new Map(this.cantidades());
    mapa.set(
      this.clave(itemLoteId, ongId),
      numero === null || Number.isNaN(numero) ? null : numero,
    );
    this.cantidades.set(mapa);
  }

  protected disponible(itemLoteId: number, ongId: number): number | undefined {
    const item = this.lote().items.find((i) => i.id === itemLoteId);
    if (!item) return undefined;
    return this.inventarioIndice().get(`${ongId}:${item.recursoId}`);
  }

  protected estado(itemLoteId: number, ongId: number) {
    return estadoCelda(this.disponible(itemLoteId, ongId), this.cantidad(itemLoteId, ongId));
  }

  protected sugerenciasCelda(itemLoteId: number, ongId: number): number[] {
    const item = this.lote().items.find((i) => i.id === itemLoteId);
    const disponible = this.disponible(itemLoteId, ongId);
    if (!item || !disponible) return [];
    return sugerencias(disponible, item.cantidadRequerida);
  }

  protected totalFila(itemLoteId: number): number {
    const valores = this.columnas().map((ong) => this.cantidad(itemLoteId, ong.id));
    return totalFila(valores);
  }

  protected filaExcedeSolicitado(itemLoteId: number): boolean {
    const item = this.lote().items.find((i) => i.id === itemLoteId);
    if (!item) return false;
    return excedeSolicitado(this.totalFila(itemLoteId), item.cantidadRequerida);
  }

  protected ongEsInvalida(ongId: number): boolean {
    return this.ongsSinAportar().includes(ongId);
  }

  protected colorOng(ongId: number): string {
    return colorOng(ongId);
  }

  protected guardar(): void {
    if (!this.formularioValido() || this.enviando()) return;

    const detalles: DetalleOfertaRequest[] = [];
    for (const item of this.lote().items) {
      for (const ong of this.columnas()) {
        const cantidad = this.cantidad(item.id, ong.id);
        if (cantidad && cantidad > 0) {
          detalles.push({ itemLoteId: item.id, ongId: ong.id, cantidadOfrecida: cantidad });
        }
      }
    }

    const ofertaEditada = this.oferta();
    const envio: Observable<OfertaResponse> = ofertaEditada
      ? this.ofertasService.actualizar(ofertaEditada.id, { detalles })
      : this.ofertasService.registrar({
          loteId: this.lote().id,
          ongIds: this.ongsSeleccionadas(),
          detalles,
        });

    this.enviando.set(true);
    this.errorMensaje.set(null);
    this.errorDetalles.set([]);

    envio.subscribe({
      next: (oferta) => {
        this.enviando.set(false);
        this.ofertaGuardada.set(oferta);
        this.paso.set('exito');
        this.guardada.emit(oferta);
      },
      error: (respuesta: HttpErrorResponse) => {
        this.enviando.set(false);
        const cuerpo = respuesta.error as ErrorBackend | undefined;
        const accion = ofertaEditada ? 'actualizar' : 'registrar';
        this.errorMensaje.set(cuerpo?.mensaje ?? `Ocurrió un error al ${accion} la oferta.`);
        this.errorDetalles.set(cuerpo?.detalles ?? []);
      },
    });
  }

  protected cerrarConExito(): void {
    this.cerrar.emit();
  }

  private clave(itemLoteId: number, ongId: number): string {
    return `${itemLoteId}:${ongId}`;
  }

  /** Modo edición: las ONGs salen de la oferta (no se pueden cambiar) y las cantidades se precargan. */
  private precargar(oferta: OfertaResponse): void {
    this.ongsDisponibles.set(oferta.ongs);
    const ongIds = oferta.ongs.map((ong) => ong.id);
    this.ongsSeleccionadas.set(ongIds);

    const mapa = new Map<string, number | null>();
    for (const aporte of oferta.aportes) {
      for (const porOng of aporte.porOng) {
        mapa.set(this.clave(aporte.itemLoteId, porOng.ongId), porOng.cantidadOfrecida);
      }
    }
    this.cantidades.set(mapa);
    this.refrescarInventario(ongIds);
  }

  private refrescarInventario(ongIds: number[]): void {
    if (ongIds.length === 0) {
      this.inventario.set([]);
      return;
    }
    this.ongsService
      .inventarioPorOng(ongIds)
      .subscribe((inventario) => this.inventario.set(inventario));
  }
}
