import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EmergenciaRequest, EmergenciaResponse } from '../../models/emergencia.model';

@Injectable({
  providedIn: 'root'
})
export class EmergenciaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/v1/emergencias';

  registrarEmergencia(emergencia: EmergenciaRequest): Observable<EmergenciaResponse> {
    return this.http.post<EmergenciaResponse>(this.apiUrl, emergencia);
  }
}