import type { EmergenciaResumen } from './emergencia';

export type EstadoLote = 'ACTIVO' | 'FINALIZADO' | 'CANCELADO';

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
