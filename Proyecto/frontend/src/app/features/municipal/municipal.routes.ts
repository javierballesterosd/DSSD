import { Routes } from '@angular/router';
import { MunicipalHome } from './municipal-home';

export const MUNICIPAL_ROUTES: Routes = [
  { path: '', component: MunicipalHome },
  // Agregá acá las rutas de este perfil, por ejemplo:
  // { path: 'emergencias/nueva', component: NuevaEmergencia },
];
