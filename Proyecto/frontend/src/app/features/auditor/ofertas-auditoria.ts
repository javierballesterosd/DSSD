import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ESTADO_OFERTA_BADGE, OfertaResponse } from '../../core/models/oferta';
import { FECHA_LISTADO } from '../../shared/formatos-fecha';
import { colorOng } from '../../shared/ong-colores';
import { OfertaDetalleModal } from '../ong/ofertas/oferta-detalle-modal';
import { Ofertas } from '../ong/services/ofertas';

/** Quita mayúsculas y acentos para comparar textos. */
function normalizar(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
}

/** Todas las ofertas (incluidas las eliminadas) con acceso a su historial de versiones. */
@Component({
  imports: [DatePipe, OfertaDetalleModal],
  selector: 'app-ofertas-auditoria',
  template: `
    <h1 class="h3 mb-3">Ofertas</h1>

    <div class="row g-2 mb-3">
      <div class="col-md-6">
        <input
          type="search"
          class="form-control"
          placeholder="Buscar por lote, zona u ONG"
          [value]="texto()"
          (input)="texto.set($any($event.target).value)"
        />
      </div>
      <div class="col-md-3">
        <select
          class="form-select"
          [value]="estado()"
          (change)="estado.set($any($event.target).value)"
        >
          <option value="">Todos los estados</option>
          @for (e of estados(); track e.estado) {
            <option [value]="e.estado">{{ e.etiqueta }}</option>
          }
        </select>
      </div>
    </div>

    @if (cargando()) {
      <p class="text-body-secondary">Cargando ofertas…</p>
    } @else if (error()) {
      <div class="alert alert-danger">No se pudieron cargar las ofertas.</div>
    } @else if (filtradas().length === 0) {
      <p class="text-body-secondary">No hay ofertas que coincidan.</p>
    } @else {
      <div class="table-responsive">
        <table class="table table-sm align-middle">
          <thead>
            <tr>
              <th>#</th>
              <th>Lote</th>
              <th>ONGs</th>
              <th>Estado</th>
              <th>Versión</th>
              <th>Registrada</th>
              <th>Última modificación</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (o of filtradas(); track o.id) {
              <tr>
                <td>{{ o.id }}</td>
                <td>
                  {{ o.loteTitulo }}
                  <div class="small text-body-secondary">{{ o.emergenciaZona }}</div>
                </td>
                <td>
                  <div class="d-flex flex-wrap gap-1">
                    @for (ong of o.ongs; track ong.id) {
                      <span class="chip-ong" [style.--ong-color]="colorOng(ong.id)">{{
                        ong.razonSocial
                      }}</span>
                    }
                  </div>
                </td>
                <td>
                  <span class="badge" [class]="badge(o.estado)">{{ o.estadoEtiqueta }}</span>
                </td>
                <td>v{{ o.numeroVersion }}</td>
                <td>{{ o.fechaOferta | date: fechaListado }}</td>
                <td>
                  {{ o.fechaModificacion ? (o.fechaModificacion | date: fechaListado) : '—' }}
                </td>
                <td class="text-end">
                  <button
                    type="button"
                    class="btn btn-outline-primary btn-sm"
                    (click)="seleccionada.set(o)"
                  >
                    Ver
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }

    @if (seleccionada(); as o) {
      <app-oferta-detalle-modal [oferta]="o" (cerrar)="seleccionada.set(null)" />
    }
  `,
})
export class OfertasAuditoria {
  private readonly ofertas = inject(Ofertas);

  protected readonly fechaListado = FECHA_LISTADO;
  protected readonly colorOng = colorOng;

  protected readonly lista = signal<OfertaResponse[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);
  protected readonly texto = signal('');
  protected readonly estado = signal('');
  protected readonly seleccionada = signal<OfertaResponse | null>(null);

  protected readonly estados = computed(() => {
    const vistos = new Map<string, string>();
    for (const o of this.lista()) {
      vistos.set(o.estado, o.estadoEtiqueta);
    }
    return [...vistos].map(([estado, etiqueta]) => ({ estado, etiqueta }));
  });

  protected readonly filtradas = computed(() => {
    const texto = normalizar(this.texto().trim());
    const estado = this.estado();
    return this.lista().filter((o) => {
      if (estado && o.estado !== estado) {
        return false;
      }
      if (!texto) {
        return true;
      }
      const contenido = [o.loteTitulo, o.emergenciaZona, ...o.ongs.map((ong) => ong.razonSocial)];
      return normalizar(contenido.join(' ')).includes(texto);
    });
  });

  constructor() {
    this.ofertas.todas().subscribe({
      next: (lista) => {
        this.lista.set(lista);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }

  protected badge(estado: string): string {
    return ESTADO_OFERTA_BADGE[estado] ?? 'text-bg-secondary';
  }
}
