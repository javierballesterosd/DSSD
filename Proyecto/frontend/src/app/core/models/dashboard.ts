/**
 * Resultado de los rankings del dashboard.
 *
 * Se utiliza para:
 * - ONGs por ofertas.
 * - ONGs por recursos asignados.
 * - Municipios por emergencias.
 * - Recursos por necesidad.
 * - Recursos por cantidad.
 */
export interface DashboardRankingResponse {
  id: number;
  nombre: string;
  total: number;
  unidadMedida: string | null;
}

/**
 * Resultado de los indicadores de tiempo de Bonita.
 */
export interface DashboardTiempo {
  etapa: string;
  cantidad: number;
  promedioSegundos: number | null;
}

/**
 * Emergencia que se encuentra en seguimiento.
 */
export interface DashboardSeguimiento {
  emergenciaId: number;
  caseId: string;
  municipio: string;
  descripcion: string;
  etapa: string;
  estado: string;
}

/**
 * Filtros compartidos por los endpoints del dashboard.
 *
 * Las fechas se envían en formato ISO: YYYY-MM-DD.
 */
export interface DashboardFiltros {
  desde?: string;
  hasta?: string;
  limite?: number;
}
