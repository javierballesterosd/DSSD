import { EstadoLote } from './lote.model';

export interface EmergenciaLoteResponse {
  id: number;
  descripcion: string;
  nivelGravedad: string;
  zonaAfectada: string;
  municipioId: number;
  fechaRegistro: string;
  loteId: number | null;
  estadoLote: EstadoLote | null;
}

export interface PaginaEmergencias {
  content: EmergenciaLoteResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
