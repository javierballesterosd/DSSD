import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

import {
  EmergenciaRequest,
  EmergenciaResponse
} from '../../../core/models/emergencia';

@Injectable({
  providedIn: 'root'
})
export class EmergenciaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/v1/emergencias`;

  registrarEmergencia(
    emergencia: EmergenciaRequest
  ): Observable<EmergenciaResponse> {
    return this.http.post<EmergenciaResponse>(
      this.apiUrl,
      emergencia
    );
  }
}