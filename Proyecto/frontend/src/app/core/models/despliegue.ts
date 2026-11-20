/** Actividad de una oferta adjudicada, tal como la ve la ONG (espejo de `MiDespliegueResponse.Actividad`). */
export interface ActividadDespliegue {
  actividadId: number;
  nombre: string;
  descripcion: string | null;
  finalizada: boolean;
  fechaFinalizacion: string | null;
  /** ONG del consorcio desde la que se marcó como hecha. */
  finalizadaPor: string | null;
  /** Username de Bonita de quien la marcó. */
  usuario: string | null;
}

/** Oferta adjudicada de la ONG con el avance de sus actividades. */
export interface MiDespliegue {
  ofertaId: number;
  estado: string;
  estadoEtiqueta: string;
  emergenciaId: number;
  emergenciaZona: string;
  nivelGravedadEtiqueta: string;
  municipio: string;
  loteId: number;
  loteTitulo: string;
  /** Todas las ONGs de la oferta; más de una implica consorcio (comparten el avance). */
  ongs: string[];
  totalActividades: number;
  actividadesFinalizadas: number;
  actividades: ActividadDespliegue[];
}
