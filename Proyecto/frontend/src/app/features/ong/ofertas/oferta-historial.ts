import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { AporteRecurso, OfertaVersion, TIPO_CAMBIO_BADGE } from '../../../core/models/oferta';
import { FECHA_LARGA } from '../../../shared/formatos-fecha';
import { Ofertas } from '../services/ofertas';
import { OfertaDesglose } from './oferta-desglose';

interface VersionConCambios {
  version: OfertaVersion;
  /** Diferencias contra la versión anterior; vacío en la primera. */
  cambios: string[];
}

/** Línea de tiempo de las versiones de una oferta, con lo que cambió en cada una. */
@Component({
  imports: [DatePipe, OfertaDesglose],
  selector: 'app-oferta-historial',
  template: `
    @if (cargando()) {
      <div class="text-center py-4">
        <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        <span class="ms-2">Cargando historial…</span>
      </div>
    } @else if (error()) {
      <div class="alert alert-danger mb-0">No se pudo cargar el historial de la oferta.</div>
    } @else if (items().length === 0) {
      <p class="text-body-secondary mb-0">Esta oferta no tiene historial registrado.</p>
    } @else {
      <ol class="list-unstyled mb-0">
        @for (item of items(); track item.version.numero) {
          <li class="border-start border-2 ps-3 pb-3">
            <div class="d-flex flex-wrap align-items-center gap-2">
              <strong>v{{ item.version.numero }}</strong>
              <span class="badge" [class]="badge(item.version.tipoCambio)">{{
                item.version.tipoCambioEtiqueta
              }}</span>
              <span class="small text-body-secondary">
                {{ item.version.fecha | date: fechaLarga }} · {{ item.version.usuario }} ({{
                  item.version.ongNombre
                }})
              </span>
            </div>
            @if (item.cambios.length > 0) {
              <ul class="small mt-1 mb-1">
                @for (cambio of item.cambios; track cambio) {
                  <li>{{ cambio }}</li>
                }
              </ul>
            }
            <details class="mt-1">
              <summary class="small">Ver cantidades de esta versión</summary>
              <div class="mt-2">
                <app-oferta-desglose [aportes]="item.version.aportes" />
              </div>
            </details>
          </li>
        }
      </ol>
    }
  `,
})
export class OfertaHistorial implements OnInit {
  private readonly ofertas = inject(Ofertas);

  readonly ofertaId = input.required<number>();

  protected readonly fechaLarga = FECHA_LARGA;
  protected readonly versiones = signal<OfertaVersion[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);

  protected readonly items = computed<VersionConCambios[]>(() => {
    const lista = this.versiones();
    // Vienen de la más nueva a la más vieja: la anterior de cada una es la siguiente de la lista.
    return lista.map((version, i) => ({
      version,
      cambios: i + 1 < lista.length ? diferencias(lista[i + 1].aportes, version.aportes) : [],
    }));
  });

  ngOnInit(): void {
    this.ofertas.versiones(this.ofertaId()).subscribe({
      next: (versiones) => {
        this.versiones.set(versiones);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }

  protected badge(tipo: string): string {
    return TIPO_CAMBIO_BADGE[tipo] ?? 'text-bg-secondary';
  }
}

/** Celdas indexadas por `itemLoteId:ongId` con su recurso, ONG y cantidad. */
function celdas(aportes: AporteRecurso[]): Map<string, { texto: string; cantidad: number }> {
  const mapa = new Map<string, { texto: string; cantidad: number }>();
  for (const aporte of aportes) {
    for (const porOng of aporte.porOng) {
      mapa.set(`${aporte.itemLoteId}:${porOng.ongId}`, {
        texto: `${aporte.recursoNombre} · ${porOng.razonSocial}`,
        cantidad: porOng.cantidadOfrecida,
      });
    }
  }
  return mapa;
}

/** Textos con lo que cambió entre dos versiones: cantidades distintas, celdas agregadas y quitadas. */
export function diferencias(anterior: AporteRecurso[], actual: AporteRecurso[]): string[] {
  const antes = celdas(anterior);
  const ahora = celdas(actual);
  const cambios: string[] = [];
  for (const [clave, celda] of ahora) {
    const previa = antes.get(clave);
    if (!previa) {
      cambios.push(`${celda.texto}: agregado (${celda.cantidad})`);
    } else if (previa.cantidad !== celda.cantidad) {
      cambios.push(`${celda.texto}: ${previa.cantidad} → ${celda.cantidad}`);
    }
  }
  for (const [clave, celda] of antes) {
    if (!ahora.has(clave)) {
      cambios.push(`${celda.texto}: quitado (${celda.cantidad})`);
    }
  }
  return cambios;
}
