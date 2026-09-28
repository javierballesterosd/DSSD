import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { LoteDetalle, LoteResumen } from '../../../core/models/lote';

@Service()
export class Lotes {
  private readonly http = inject(HttpClient);

  listarPublicados(): Observable<LoteResumen[]> {
    return this.http.get<LoteResumen[]>(`${environment.apiUrl}/lotes`);
  }

  obtenerDetalle(id: number): Observable<LoteDetalle> {
    return this.http.get<LoteDetalle>(`${environment.apiUrl}/lotes/${id}`);
  }
}
