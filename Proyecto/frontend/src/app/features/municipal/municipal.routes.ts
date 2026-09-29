import { Routes } from '@angular/router';
import { MunicipalHome } from './municipal-home';
import { AltaEmergenciaComponent } from './alta-emergencia/alta-emergencia';

export const MUNICIPAL_ROUTES: Routes = [
  {
    path: '',
    component: MunicipalHome
  },
  {
    path: 'emergencias/nueva',
    component: AltaEmergenciaComponent
  }
];