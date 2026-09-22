import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { homeRedirectGuard } from './core/guards/home-redirect-guard';
import { MainLayout } from './layout/main-layout/main-layout';
import { NotFound } from './shared/components/not-found/not-found';

/**
 * Cada perfil tiene su propio archivo de rutas en features/<perfil>/<perfil>.routes.ts.
 * Para agregar una pantalla, editá solo el archivo de tu perfil (ver README).
 */
export const routes: Routes = [
  {
    path: 'login',
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', canActivate: [homeRedirectGuard], children: [] },
      {
        path: 'municipal',
        loadChildren: () =>
          import('./features/municipal/municipal.routes').then((m) => m.MUNICIPAL_ROUTES),
      },
      {
        path: 'coordinador',
        loadChildren: () =>
          import('./features/coordinador/coordinador.routes').then((m) => m.COORDINADOR_ROUTES),
      },
      {
        path: 'ong',
        loadChildren: () => import('./features/ong/ong.routes').then((m) => m.ONG_ROUTES),
      },
      {
        path: 'auditor',
        loadChildren: () =>
          import('./features/auditor/auditor.routes').then((m) => m.AUDITOR_ROUTES),
      },
      { path: '**', component: NotFound },
    ],
  },
];
