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