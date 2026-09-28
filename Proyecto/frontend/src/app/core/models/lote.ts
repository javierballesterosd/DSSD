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
  /** Ventana de recepción de ofertas de este lote. */
  fechaAperturaOfertas: string | null;
  fechaCierreOfertas: string | null;
  emergencia: EmergenciaResumen;
  items: ItemLote[];
}

export interface LoteDetalle extends LoteResumen {
  convocatoriaAbierta: boolean;
}
