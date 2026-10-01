import { Routes } from '@angular/router';
import { MunicipalHome } from './municipal-home';
import { AltaEmergencia } from './alta-emergencia/alta-emergencia';

export const MUNICIPAL_ROUTES: Routes = [
  { path: '', component: MunicipalHome },
  { path: 'emergencias/nueva', component: AltaEmergencia },
];
