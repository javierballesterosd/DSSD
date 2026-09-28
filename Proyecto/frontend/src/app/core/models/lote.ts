import { EmergenciaResumen } from './emergencia';

export interface ItemLote {
  id: number;
  recursoId: number;
  recursoNombre: string;
  unidadMedida: string;
  cantidadRequerida: number;
}

export interface LoteResumen {
  id: number;
  titulo: string;
  estado: string;
  fechaCreacion: string;
  fechaInicio: string | null;
  emergencia: EmergenciaResumen;
  items: ItemLote[];
}

export interface LoteDetalle extends LoteResumen {
  convocatoriaAbierta: boolean;
}
