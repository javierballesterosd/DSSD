import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LoteDetalle } from '../../../core/models/lote';
import { DetalleOfertaRequest, OfertaRequest, OfertaResponse } from '../../../core/models/oferta';
import { InventarioOng, Ong } from '../../../core/models/ong';
import { Modal } from '../../../shared/components/modal/modal';
import { Ofertas } from '../services/ofertas';
import { Ongs } from '../services/ongs';
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

/** Paleta para distinguir ONGs de un vistazo; sin verde, reservado para el aviso de éxito. */
const PALETA_ONG = ['text-bg-primary', 'text-bg-danger', 'text-bg-warning', 'text-bg-info', 'text-bg-dark'];

@Component({
  imports: [Modal, FormsModule],
  selector: 'app-oferta-form-modal',
  templateUrl: './oferta-form-modal.html',
  styleUrl: './oferta-form-modal.scss',
})
export class OfertaFormModal implements OnInit {
  private readonly ongsService = inject(Ongs);
  private readonly ofertasService = inject(Ofertas);

  readonly lote = input.required<LoteDetalle>();
  readonly registrada = output<OfertaResponse>();
  readonly cerrar = output<void>();

  protected readonly paso = signal<'formulario' | 'exito'>('formulario');
  protected readonly ongsDisponibles = signal<Ong[]>([]);
  protected readonly ongsSeleccionadas = signal<number[]>([]);
  protected readonly inventario = signal<InventarioOng[]>([]);
  protected readonly cantidades = signal<Map<string, number | null>>(new Map());
  protected readonly enviando = signal(false);
  protected readonly errorMensaje = signal<string | null>(null);
  protected readonly errorDetalles = signal<string[]>([]);
  protected readonly ofertaRegistrada = signal<OfertaResponse | null>(null);

  protected readonly inventarioIndice = computed(() => indexarInventario(this.inventario()));

  protected readonly columnas = computed(() =>
    this.ongsDisponibles().filter((ong) => this.ongsSeleccionadas().includes(ong.id)),
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
      const ong = this.ongsDisponibles().find((o) => o.id === ongId);
      motivos.push(
        `«${ong?.razonSocial}» no está aportando nada: cargale una cantidad o sacala de la selección.`,
      );
    }
    return motivos;
  });

  protected readonly formularioValido = computed(() => this.motivos().length === 0);

  ngOnInit(): void {
    this.ongsService.listar().subscribe((ongs) => this.ongsDisponibles.set(ongs));
  }

  protected toggleOng(ongId: number, marcado: boolean): void {
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

  /** Color estable por ONG (según su id), para distinguirlas de un vistazo en la grilla y el resumen. */
  protected colorOng(ongId: number): string {
    return PALETA_ONG[ongId % PALETA_ONG.length];
  }

  protected registrar(): void {
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

    const request: OfertaRequest = {
      loteId: this.lote().id,
      ongIds: this.ongsSeleccionadas(),
      detalles,
    };

    this.enviando.set(true);
    this.errorMensaje.set(null);
    this.errorDetalles.set([]);

    this.ofertasService.registrar(request).subscribe({
      next: (oferta) => {
        this.enviando.set(false);
        this.ofertaRegistrada.set(oferta);
        this.paso.set('exito');
        this.registrada.emit(oferta);
      },
      error: (respuesta: HttpErrorResponse) => {
        this.enviando.set(false);
        const cuerpo = respuesta.error as ErrorBackend | undefined;
        this.errorMensaje.set(cuerpo?.mensaje ?? 'Ocurrió un error al registrar la oferta.');
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
