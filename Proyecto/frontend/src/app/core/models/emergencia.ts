import type { EstadoLote } from './lote';

export const NIVELES_GRAVEDAD = ['BAJA', 'MEDIA', 'ALTA', 'CRITICA'] as const;

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

export interface ActividadRequest {
  nombre: string;
  descripcion: string;
}

export interface ActividadResponse {
  id: number;
  nombre: string;
  descripcion: string;
  finalizada: boolean;
}

export interface EmergenciaRequest {
  nivelGravedad: NivelGravedad;
  zonaAfectada: string;
  descripcion: string;
  actividades: ActividadRequest[];
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
  requiereActualizacion?: boolean;
  /** Hay que volver a registrar porque el SN no habilitó recursos (y no porque el municipio rechazó). */
  sinRecursos?: boolean;
  /** Por qué se canceló el último lote (solo si está cancelado). */
  motivoLoteCancelado?: 'SIN_RECURSOS' | 'RECHAZO_MUNICIPAL' | null;
  actividades: ActividadResponse[];
}

export interface RecursosMunicipalesResponse {
  loteId: number | null;
  decision: boolean | null;
  confirmada: boolean;
  puedeDecidir: boolean;
  mensaje: string | null;
  recursos: {
    recursoId: number;
    nombre: string;
    unidadMedida: string;
    /** Lo que pedía el lote de este recurso. */
    cantidadRequerida: number;
    /** Lo que habilitó el Sistema Nacional. */
    cantidad: number;
    /** Cuánto aporta cada ONG de la cantidad habilitada. */
    porOng: { ongId: number; razonSocial: string; cantidad: number }[];
  }[];
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

/** Estado del cierre, calculado por el backend para el coordinador de la región. */
export interface CierreDespliegueResponse {
  emergenciaId: number;
  municipio: string;
  zonaAfectada: string;
  descripcion: string;
  actividadesTotales: number;
  actividadesPendientes: number;
  adjudicada: boolean;
  validacionRegistrada: boolean;
  finalizada: boolean;
  descripcionCierre: string | null;
  fechaValidacion: string | null;
  fechaFinalizacion: string | null;
}
