import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Recurso } from '../../models/recurso.model';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class RecursoService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/recursos`;

  obtenerRecursos(): Observable<Recurso[]> {
    return this.http.get<Recurso[]>(this.apiUrl);
  }
}
