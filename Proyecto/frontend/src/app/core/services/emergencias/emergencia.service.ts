import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EmergenciaRequest, EmergenciaResponse } from '../../models/emergencia.model';
import { PaginaEmergencias } from '../../models/emergencia-lote.model';

@Injectable({
  providedIn: 'root',
})
export class EmergenciaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/v1/emergencias';

  registrarEmergencia(emergencia: EmergenciaRequest): Observable<EmergenciaResponse> {
    return this.http.post<EmergenciaResponse>(this.apiUrl, emergencia);
  }
  obtenerEmergenciasParaLotes(page: number = 0, size: number = 10): Observable<PaginaEmergencias> {
    return this.http.get<PaginaEmergencias>(`${this.apiUrl}/para-lotes?page=${page}&size=${size}`);
  }
}
