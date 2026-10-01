import type { EmergenciaResumen } from './emergencia';

export type EstadoLote = 'ACTIVO' | 'FINALIZADO' | 'CANCELADO';

export const ESTADO_LOTE_LABEL: Record<EstadoLote, string> = {
  ACTIVO: 'Lote activo',
  FINALIZADO: 'Lote finalizado',
  CANCELADO: 'Lote cancelado',
};

/** Clase de badge Bootstrap según el estado del lote. */
export const ESTADO_LOTE_BADGE: Record<EstadoLote, string> = {
  ACTIVO: 'text-bg-success',
  FINALIZADO: 'text-bg-primary',
  CANCELADO: 'text-bg-danger',
};

export interface ItemLoteRequest {
  recursoId: number;
  cantidadRequerida: number;
}

export interface ItemLote {
  id: number;
  recursoId: number;
  recursoNombre: string;
  unidadMedida: string;
  cantidadRequerida: number;
}

export interface LoteRequest {
  titulo: string;
  fechaAperturaOfertas: string;
  fechaCierreOfertas: string;
  items: ItemLoteRequest[];
}

export interface LoteResumen {
  id: number;
  titulo: string;
  estado: string;
  fechaCreacion: string;
  /** Ventana de recepción de ofertas de este lote. */
  fechaAperturaOfertas: string | null;
  fechaCierreOfertas: string | null;
  emergencia: EmergenciaResumen;
  items: ItemLote[];
}

export interface LoteDetalle extends LoteResumen {
  convocatoriaAbierta: boolean;
}
