import { HttpClient, HttpParams } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  OfertaEdicionRequest,
  OfertaRequest,
  OfertaResponse,
  OfertaVersion,
} from '../../../core/models/oferta';

@Service()
export class Ofertas {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /** Ofertas del lote en las que participa la ONG del usuario logueado. */
  misOfertas(loteId: number): Observable<OfertaResponse[]> {
    return this.http.get<OfertaResponse[]>(`${this.apiUrl}/ofertas/mias`, {
      params: new HttpParams().set('loteId', loteId),
    });
  }

  /** Ids de los lotes donde la ONG del usuario logueado ya ofertó (oferta vigente). */
  lotesConMisOfertas(): Observable<number[]> {
    return this.http.get<number[]>(`${this.apiUrl}/ofertas/mias/lotes`);
  }

  /** Historial de versiones de una oferta, de la más nueva a la más vieja. */
  versiones(id: number): Observable<OfertaVersion[]> {
    return this.http.get<OfertaVersion[]>(`${this.apiUrl}/ofertas/${id}/versiones`);
  }

  /** Todas las ofertas, incluidas las eliminadas (solo auditor). */
  todas(loteId?: number): Observable<OfertaResponse[]> {
    let params = new HttpParams();
    if (loteId !== undefined) {
      params = params.set('loteId', loteId);
    }
    return this.http.get<OfertaResponse[]>(`${this.apiUrl}/ofertas`, { params });
  }

  registrar(request: OfertaRequest): Observable<OfertaResponse> {
    return this.http.post<OfertaResponse>(`${this.apiUrl}/ofertas`, request);
  }

  /** Cambia las cantidades de una oferta pendiente (dentro de la ventana del lote). */
  actualizar(id: number, request: OfertaEdicionRequest): Observable<OfertaResponse> {
    return this.http.put<OfertaResponse>(`${this.apiUrl}/ofertas/${id}`, request);
  }

  /** Baja lógica: la oferta queda en estado ELIMINADA. */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/ofertas/${id}`);
  }
}
