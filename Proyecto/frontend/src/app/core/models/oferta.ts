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

/** Edición: solo cambian las cantidades; las ONGs participantes quedan fijas. */
export interface OfertaEdicionRequest {
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
  estadoEtiqueta: string;
  fechaOferta: string;
  /** Última edición o baja; null si nunca se modificó. */
  fechaModificacion: string | null;
  /** Número de la última versión registrada (1 = solo el alta). */
  numeroVersion: number;
  loteId: number;
  loteTitulo: string;
  emergenciaZona: string;
  ongs: Ong[];
  aportes: AporteRecurso[];
}

/** Foto de la oferta tras un alta, una edición o una baja. */
export interface OfertaVersion {
  numero: number;
  tipoCambio: string;
  tipoCambioEtiqueta: string;
  estado: string;
  estadoEtiqueta: string;
  fecha: string;
  /** Username de Bonita de quien hizo el cambio. */
  usuario: string;
  ongId: number;
  ongNombre: string;
  aportes: AporteRecurso[];
}

/** Clase Bootstrap del badge de cada tipo de cambio de una versión. */
export const TIPO_CAMBIO_BADGE: Record<string, string> = {
  CREACION: 'text-bg-success',
  EDICION: 'text-bg-info',
  BAJA: 'text-bg-danger',
};

/** Clase Bootstrap del badge de cada estado de oferta. */
export const ESTADO_OFERTA_BADGE: Record<string, string> = {
  PENDIENTE: 'text-bg-secondary',
  ACEPTADA_PARCIAL: 'text-bg-info',
  VALIDADA: 'text-bg-success',
  ADJUDICADA: 'text-bg-success',
  NO_ADJUDICADA: 'text-bg-secondary',
  EN_EJECUCION: 'text-bg-primary',
  FINALIZADA: 'text-bg-dark',
  RECHAZADA: 'text-bg-danger',
  ELIMINADA: 'text-bg-light',
};
