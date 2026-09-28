import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { OfertaRequest, OfertaResponse } from '../../../core/models/oferta';

@Service()
export class Ofertas {
  private readonly http = inject(HttpClient);

  registrar(request: OfertaRequest): Observable<OfertaResponse> {
    return this.http.post<OfertaResponse>(`${environment.apiUrl}/ofertas`, request);
  }
}
