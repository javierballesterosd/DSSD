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

/** Clase de badge según la gravedad (Bootstrap; `text-bg-critica` está en styles.scss). */
export const NIVEL_GRAVEDAD_BADGE: Record<NivelGravedad, string> = {
  BAJA: 'text-bg-secondary',
  MEDIA: 'text-bg-warning',
  ALTA: 'text-bg-danger',
  CRITICA: 'text-bg-critica',
};

/** Color según la gravedad, para armar `bg-<color>-subtle` (`critica` está en styles.scss). */
export const NIVEL_GRAVEDAD_COLOR: Record<NivelGravedad, string> = {
  BAJA: 'secondary',
  MEDIA: 'warning',
  ALTA: 'danger',
  CRITICA: 'critica',
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

/** Emergencia en los listados y el detalle de municipal y coordinador, con su último lote (null si todavía no tiene). */
export interface EmergenciaParaLoteResponse {
  id: number;
  descripcion: string;
  nivelGravedad: NivelGravedad;
  nivelGravedadEtiqueta: string;
  zonaAfectada: string;
  municipioId: number;
  municipio: string;
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
