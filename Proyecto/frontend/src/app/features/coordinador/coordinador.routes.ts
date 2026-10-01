import { Routes } from '@angular/router';
import { CoordinadorHome } from './coordinador-home';
import { PublicacionLotes } from './publicacion-lotes/publicacion-lotes';

export const COORDINADOR_ROUTES: Routes = [
  { path: '', component: CoordinadorHome },
  { path: 'lotes', component: PublicacionLotes },
];
