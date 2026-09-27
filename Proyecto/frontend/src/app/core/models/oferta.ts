import { Ong } from './ong';

export interface DetalleOfertaRequest {
  itemLoteId: number;
  ongId: number;
  cantidadOfrecida: number;
}

export interface OfertaRequest {
  loteId: number;
  ongIds: number[];
  detalles: DetalleOfertaRequest[];
}

export interface AporteOng {
  ongId: number;
  razonSocial: string;
  cantidadOfrecida: number;
}

export interface AporteRecurso {
  itemLoteId: number;
  recursoNombre: string;
  unidadMedida: string;
  cantidadRequerida: number;
  totalOfrecido: number;
  porOng: AporteOng[];
}

export interface OfertaResponse {
  id: number;
  estado: string;
  fechaOferta: string;
  loteId: number;
  loteTitulo: string;
  emergenciaZona: string;
  ongs: Ong[];
  aportes: AporteRecurso[];
}
