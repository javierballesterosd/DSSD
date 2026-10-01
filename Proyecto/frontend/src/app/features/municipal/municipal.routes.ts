import { Routes } from '@angular/router';
import { AltaEmergencia } from './alta-emergencia/alta-emergencia';
import { EmergenciaDetalle } from './emergencias/emergencia-detalle';
import { MisEmergencias } from './emergencias/mis-emergencias';

export const MUNICIPAL_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'emergencias' },
  { path: 'emergencias', component: MisEmergencias },
  { path: 'emergencias/nueva', component: AltaEmergencia },
  { path: 'emergencias/:id', component: EmergenciaDetalle },
];
