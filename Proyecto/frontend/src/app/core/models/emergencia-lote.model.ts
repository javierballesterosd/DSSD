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
