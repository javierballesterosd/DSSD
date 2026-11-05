import { Routes } from '@angular/router';
import { OfertasAuditoria } from './ofertas-auditoria';
import { Dashboard } from './dashboard/dashboard';

export const AUDITOR_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'ofertas' },
  { path: 'ofertas', component: OfertasAuditoria },
  { path: 'dashboard', component: Dashboard },
  // Agregá acá las rutas de este perfil, por ejemplo:
  // { path: 'indicadores', component: Indicadores },
];
