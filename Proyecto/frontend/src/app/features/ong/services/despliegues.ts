import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ActividadDespliegue, MiDespliegue } from '../../../core/models/despliegue';

@Service()
export class Despliegues {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /** Ofertas adjudicadas en curso de la ONG del usuario logueado, con el avance de sus actividades. */
  misDespliegues(): Observable<MiDespliegue[]> {
    return this.http.get<MiDespliegue[]>(`${this.apiUrl}/despliegues/mios`);
  }

  /** Marca la actividad como hecha para toda la oferta (la comparten las ONGs del consorcio). */
  finalizarActividad(ofertaId: number, actividadId: number): Observable<ActividadDespliegue> {
    return this.http.post<ActividadDespliegue>(
      `${this.apiUrl}/ofertas/${ofertaId}/actividades/${actividadId}/finalizacion`,
      {},
    );
  }
}
