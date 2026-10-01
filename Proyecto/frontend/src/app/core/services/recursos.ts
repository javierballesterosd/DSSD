import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Recurso } from '../models/recurso';

@Service()
export class Recursos {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/recursos`;

  listar(): Observable<Recurso[]> {
    return this.http.get<Recurso[]>(this.apiUrl);
  }
}
