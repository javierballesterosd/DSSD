import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoteDetalle, LoteRequest, LoteResumen } from '../models/lote';

@Service()
export class Lotes {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  listarPublicados(): Observable<LoteResumen[]> {
    return this.http.get<LoteResumen[]>(`${this.apiUrl}/lotes`);
  }

  obtenerDetalle(id: number): Observable<LoteDetalle> {
    return this.http.get<LoteDetalle>(`${this.apiUrl}/lotes/${id}`);
  }

  publicar(emergenciaId: number, lote: LoteRequest): Observable<LoteDetalle> {
    return this.http.post<LoteDetalle>(`${this.apiUrl}/emergencias/${emergenciaId}/lotes`, lote);
  }
}
