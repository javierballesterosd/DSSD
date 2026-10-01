import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Recurso } from '../../models/recurso.model';

@Injectable({
  providedIn: 'root',
})
export class RecursoService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/recursos';

  obtenerRecursos(): Observable<Recurso[]> {
    return this.http.get<Recurso[]>(this.apiUrl);
  }
}
