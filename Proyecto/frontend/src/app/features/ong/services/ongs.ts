import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { InventarioOng, Ong } from '../../../core/models/ong';

@Service()
export class Ongs {
  private readonly http = inject(HttpClient);

  listar(): Observable<Ong[]> {
    return this.http.get<Ong[]>(`${environment.apiUrl}/ongs`);
  }

  inventarioPorOng(ongIds: number[]): Observable<InventarioOng[]> {
    return this.http.get<InventarioOng[]>(`${environment.apiUrl}/ongs/inventario`, {
      params: { ongIds: ongIds.join(',') },
    });
  }
}
