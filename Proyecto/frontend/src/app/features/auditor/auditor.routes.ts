import { Routes } from '@angular/router';
import { AuditorHome } from './auditor-home';
import { OfertasAuditoria } from './ofertas-auditoria';

export const AUDITOR_ROUTES: Routes = [
  { path: '', component: AuditorHome },
  { path: 'ofertas', component: OfertasAuditoria },
  // Agregá acá las rutas de este perfil, por ejemplo:
  // { path: 'indicadores', component: Indicadores },
];
