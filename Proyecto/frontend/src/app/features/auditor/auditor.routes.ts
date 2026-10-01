import { Routes } from '@angular/router';
import { OfertasAuditoria } from './ofertas-auditoria';

export const AUDITOR_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'ofertas' },
  { path: 'ofertas', component: OfertasAuditoria },
  // Agregá acá las rutas de este perfil, por ejemplo:
  // { path: 'indicadores', component: Indicadores },
];
