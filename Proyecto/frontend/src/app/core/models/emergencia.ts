import type { EstadoLote } from './lote';

export const NIVELES_GRAVEDAD = [
  'BAJA',
  'MEDIA',
  'ALTA',
  'CRITICA'
] as const;

export type NivelGravedad = (typeof NIVELES_GRAVEDAD)[number];

export const NIVEL_GRAVEDAD_LABEL: Record<NivelGravedad, string> = {
  BAJA: 'Baja',
  MEDIA: 'Media',
  ALTA: 'Alta',
  CRITICA: 'Crítica',
};

/** Clase de badge Bootstrap según la gravedad. */
export const NIVEL_GRAVEDAD_BADGE: Record<NivelGravedad, string> = {
  BAJA: 'text-bg-secondary',
  MEDIA: 'text-bg-warning',
  ALTA: 'text-bg-danger',
  CRITICA: 'text-bg-dark',
};

/** Color de Bootstrap según la gravedad. */
export const NIVEL_GRAVEDAD_COLOR: Record<NivelGravedad, string> = {
  BAJA: 'secondary',
  MEDIA: 'warning',
  ALTA: 'danger',
  CRITICA: 'dark',
};

export interface EmergenciaRequest {
  nivelGravedad: NivelGravedad;
  zonaAfectada: string;
  descripcion: string;
}

export interface EmergenciaResponse {
  id: number;
  nivelGravedad: NivelGravedad;
  zonaAfectada: string;
  descripcion: string;
  fechaRegistro: string;
  bonitaCaseId?: string;
  municipioId: number;
}

export interface EmergenciaResumen {
  id: number;
  zonaAfectada: string;
  nivelGravedad: NivelGravedad;
  nivelGravedadEtiqueta: string;
  descripcion: string;
  fechaRegistro: string;
  municipio: string;
}

/** Emergencia en la lista del coordinador, con su último lote (null si todavía no tiene). */
export interface EmergenciaParaLoteResponse {
  id: number;
  descripcion: string;
  nivelGravedad: NivelGravedad;
  zonaAfectada: string;
  municipioId: number;
  fechaRegistro: string;
  loteId: number | null;
  estadoLote: EstadoLote | null;
}

export interface PaginaEmergencias {
  content: EmergenciaParaLoteResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
