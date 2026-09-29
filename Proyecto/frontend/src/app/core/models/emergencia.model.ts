export type NivelGravedad = 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA';

export interface NivelGravedadInfo {
  clave: NivelGravedad;
  etiqueta: string;
  descripcion: string;
}

export const NIVELES_GRAVEDAD: NivelGravedadInfo[] = [
  {
    clave: 'BAJA',
    etiqueta: 'Baja',
    descripcion: 'Afecta a pocas familias, sin riesgo para la vida',
  },
  {
    clave: 'MEDIA',
    etiqueta: 'Media',
    descripcion:
      'Afecta a varios barrios; hay daños materiales y riesgo limitado para las personas',
  },
  {
    clave: 'ALTA',
    etiqueta: 'Alta',
    descripcion:
      'Afecta a gran parte del municipio; hay personas en riesgo y servicios básicos interrumpidos',
  },
  {
    clave: 'CRITICA',
    etiqueta: 'Crítica',
    descripcion:
      'Riesgo de vida inmediato para muchas personas; requiere respuesta regional urgente',
  },
];

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
