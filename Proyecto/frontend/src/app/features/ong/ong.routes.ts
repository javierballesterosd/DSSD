import { Routes } from '@angular/router';
import { MisDespliegues } from './despliegues/mis-despliegues';
import { LoteDetalle } from './lotes/lote-detalle';
import { LotesDisponibles } from './lotes/lotes-disponibles';

export const ONG_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'lotes' },
  { path: 'lotes', component: LotesDisponibles },
  { path: 'lotes/:id', component: LoteDetalle },
  { path: 'despliegues', component: MisDespliegues },
];
