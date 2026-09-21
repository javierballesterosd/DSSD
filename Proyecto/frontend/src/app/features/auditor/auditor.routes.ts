import { Routes } from '@angular/router';
import { AuditorHome } from './auditor-home';

export const AUDITOR_ROUTES: Routes = [
  { path: '', component: AuditorHome },
  // Agregá acá las rutas de este perfil, por ejemplo:
  // { path: 'indicadores', component: Indicadores },
];
