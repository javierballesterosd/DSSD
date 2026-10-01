import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EmergenciaRequest, EmergenciaResponse, PaginaEmergencias } from '../models/emergencia';

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
}
