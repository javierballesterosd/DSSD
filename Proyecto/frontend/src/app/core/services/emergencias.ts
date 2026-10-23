import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  EmergenciaParaLoteResponse,
  EmergenciaRequest,
  EmergenciaResponse,
  PaginaEmergencias,
  RecursosMunicipalesResponse,
} from '../models/emergencia';

@Service()
export class Emergencias {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/emergencias`;

  registrar(emergencia: EmergenciaRequest): Observable<EmergenciaResponse> {
    return this.http.post<EmergenciaResponse>(this.apiUrl, emergencia);
  }

  listarParaLotes(page = 0, size = 10): Observable<PaginaEmergencias> {
    return this.http.get<PaginaEmergencias>(`${this.apiUrl}/para-lotes`, {
      params: { page, size },
    });
  }

  /** Emergencias del municipio del operador logueado. */
  listarMias(): Observable<EmergenciaParaLoteResponse[]> {
    return this.http.get<EmergenciaParaLoteResponse[]>(`${this.apiUrl}/mias`);
  }

  obtener(id: number): Observable<EmergenciaParaLoteResponse> {
    return this.http.get<EmergenciaParaLoteResponse>(`${this.apiUrl}/${id}`);
  }

  recursosAceptados(id: number): Observable<RecursosMunicipalesResponse> {
    return this.http.get<RecursosMunicipalesResponse>(`${this.apiUrl}/${id}/recursos-aceptados`);
  }

  decidir(id: number, loteId: number, aceptar: boolean): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/decision-municipal`, { loteId, aceptar });
  }

  actualizar(id: number, emergencia: EmergenciaRequest): Observable<EmergenciaResponse> {
    return this.http.put<EmergenciaResponse>(`${this.apiUrl}/${id}`, emergencia);
  }
}
