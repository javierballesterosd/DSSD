import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { LoteRequest, LoteResponse } from '../../models/lote.model';

@Injectable({
  providedIn: 'root',
})
export class LoteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/emergencias';

  publicarLote(emergenciaId: number, lote: LoteRequest): Observable<LoteResponse> {
    return this.http.post<LoteResponse>(`${this.apiUrl}/${emergenciaId}/lotes`, lote);
  }
}
