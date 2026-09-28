import { HttpClient, HttpParams } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { OfertaRequest, OfertaResponse } from '../../../core/models/oferta';

@Service()
export class Ofertas {
  private readonly http = inject(HttpClient);

  /** Ofertas del lote en las que participa la ONG del usuario logueado. */
  misOfertas(loteId: number): Observable<OfertaResponse[]> {
    return this.http.get<OfertaResponse[]>(`${environment.apiUrl}/ofertas/mias`, {
      params: new HttpParams().set('loteId', loteId),
    });
  }

  registrar(request: OfertaRequest): Observable<OfertaResponse> {
    return this.http.post<OfertaResponse>(`${environment.apiUrl}/ofertas`, request);
  }
}
