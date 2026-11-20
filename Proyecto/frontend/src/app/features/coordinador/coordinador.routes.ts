import { Routes } from '@angular/router';
import { CierreDespliegue } from './lotes/cierre-despliegue';
import { EmergenciasRegion } from './emergencias/emergencias-region';
import { LoteDetalle } from './lotes/lote-detalle';
import { MisLotes } from './lotes/mis-lotes';
import { PublicacionLotes } from './publicacion-lotes/publicacion-lotes';

export const COORDINADOR_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'emergencias' },
  { path: 'emergencias', component: EmergenciasRegion },
  { path: 'emergencias/:id/cierre', component: CierreDespliegue },
  { path: 'emergencias/:id/lote/nuevo', component: PublicacionLotes },
  { path: 'lotes', component: MisLotes },
  { path: 'lotes/:id', component: LoteDetalle },
];
