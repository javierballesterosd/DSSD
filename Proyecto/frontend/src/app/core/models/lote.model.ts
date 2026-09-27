import { ItemLoteRequest } from './item-lote.model';
import { ItemLoteResponse } from './item-lote.model';

export type EstadoLote = 'ACTIVO' | 'FINALIZADO' | 'CANCELADO';

export interface LoteRequest {
  titulo: string;
  fechaInicio?: string;
  items: ItemLoteRequest[];
}

export interface LoteResponse {
  id: number;
  titulo: string;
  estado: EstadoLote;
  fechaCreacion: string;
  fechaInicio?: string;
  emergenciaId: number;
  items: ItemLoteResponse[];
}
