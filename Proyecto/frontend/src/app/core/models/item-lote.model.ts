export interface ItemLoteRequest {
  recursoId: number;
  cantidadRequerida: number;
}

export interface ItemLoteResponse {
  id: number;
  recursoId: number;
  recursoNombre: string;
  unidadMedida: string;
  cantidadRequerida: number;
}
