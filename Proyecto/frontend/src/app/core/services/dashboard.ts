import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

import {
  DashboardFiltros,
  DashboardRankingResponse,
  DashboardSeguimiento,
  DashboardTiempo,
} from '../models/dashboard';

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${environment.apiUrl}/dashboard`;

  /**
   * Construye los parámetros compartidos por los endpoints.
   */
  private construirParams(filtros: DashboardFiltros = {}): HttpParams {
    let params = new HttpParams();

    if (filtros.desde) {
      params = params.set('desde', filtros.desde);
    }

    if (filtros.hasta) {
      params = params.set('hasta', filtros.hasta);
    }

    if (filtros.limite !== undefined) {
      params = params.set('limite', filtros.limite);
    }

    return params;
  }

  // ============================================================
  // ORGANIZACIONES
  // ============================================================

  /**
   * Ranking de ONGs según la cantidad de ofertas registradas.
   */
  ongsPorOfertas(
    filtros: DashboardFiltros = {},
  ): Observable<DashboardRankingResponse[]> {
    return this.http.get<DashboardRankingResponse[]>(
      `${this.apiUrl}/ongs/ofertas`,
      { params: this.construirParams(filtros) },
    );
  }

  /**
   * Ranking de ONGs según los recursos asignados.
   */
  ongsPorRecursosAsignados(
    filtros: DashboardFiltros = {},
  ): Observable<DashboardRankingResponse[]> {
    return this.http.get<DashboardRankingResponse[]>(
      `${this.apiUrl}/ongs/recursos-asignados`,
      { params: this.construirParams(filtros) },
    );
  }

  // ============================================================
  // EMERGENCIAS
  // ============================================================

  /**
   * Ranking de municipios según la cantidad de emergencias.
   */
  municipiosPorEmergencias(
    filtros: DashboardFiltros = {},
  ): Observable<DashboardRankingResponse[]> {
    return this.http.get<DashboardRankingResponse[]>(
      `${this.apiUrl}/municipios/emergencias`,
      { params: this.construirParams(filtros) },
    );
  }

  // ============================================================
  // RECURSOS
  // ============================================================

  /**
   * Ranking de recursos según su nivel de necesidad.
   */
  recursosPorNecesidad(
    filtros: DashboardFiltros = {},
  ): Observable<DashboardRankingResponse[]> {
    return this.http.get<DashboardRankingResponse[]>(
      `${this.apiUrl}/recursos/necesidad`,
      { params: this.construirParams(filtros) },
    );
  }

  /**
   * Cantidad de recursos registrados.
   *
   * Este endpoint no admite filtros de fecha.
   */
  recursosPorCantidad(limite?: number): Observable<DashboardRankingResponse[]> {
    let params = new HttpParams();

    if (limite !== undefined) {
      params = params.set('limite', limite);
    }

    return this.http.get<DashboardRankingResponse[]>(
      `${this.apiUrl}/recursos/cantidad`,
      { params },
    );
  }

  // ============================================================
  // TIEMPOS DE GESTIÓN
  // ============================================================

  /**
   * Duración promedio de las distintas etapas del proceso.
   */
  etapas(
    filtros: DashboardFiltros = {},
  ): Observable<DashboardTiempo[]> {
    return this.http.get<DashboardTiempo[]>(
      `${this.apiUrl}/etapas/tiempos`,
      { params: this.construirParams(filtros) },
    );
  }

  /**
   * Tiempo promedio de validación nacional.
   */
  tiempoValidacionNacional(
    filtros: DashboardFiltros = {},
  ): Observable<DashboardTiempo> {
    return this.http.get<DashboardTiempo>(
      `${this.apiUrl}/tiempos/validacion-nacional`,
      { params: this.construirParams(filtros) },
    );
  }

  /**
   * Tiempo promedio de decisión municipal.
   */
  tiempoDecisionMunicipal(
    filtros: DashboardFiltros = {},
  ): Observable<DashboardTiempo> {
    return this.http.get<DashboardTiempo>(
      `${this.apiUrl}/tiempos/decision-municipal`,
      { params: this.construirParams(filtros) },
    );
  }

  /**
   * Tiempo promedio de registro de compromiso.
   */
  tiempoCompromiso(
    filtros: DashboardFiltros = {},
  ): Observable<DashboardTiempo> {
    return this.http.get<DashboardTiempo>(
      `${this.apiUrl}/tiempos/compromiso`,
      { params: this.construirParams(filtros) },
    );
  }

  /**
   * Tiempo promedio hasta la finalización de emergencias.
   */
  tiempoEmergenciasFinalizadas(
    filtros: DashboardFiltros = {},
  ): Observable<DashboardTiempo> {
    return this.http.get<DashboardTiempo>(
      `${this.apiUrl}/tiempos/emergencias-finalizadas`,
      { params: this.construirParams(filtros) },
    );
  }

  // ============================================================
  // SEGUIMIENTO
  // ============================================================

  /**
   * Emergencias que actualmente aparecen en la etapa de seguimiento.
   */
  seguimiento(
    filtros: DashboardFiltros = {},
  ): Observable<DashboardSeguimiento[]> {
    return this.http.get<DashboardSeguimiento[]>(
      `${this.apiUrl}/emergencias/seguimiento`,
      { params: this.construirParams(filtros) },
    );
  }
}
