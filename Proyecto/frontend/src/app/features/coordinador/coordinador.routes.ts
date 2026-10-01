import { Routes } from '@angular/router';
import { CoordinadorHome } from './coordinador-home';
import { PublicacionLotesComponent } from './publicacion-lotes/publicacion-lotes.component';

export const COORDINADOR_ROUTES: Routes = [
  { path: '', component: CoordinadorHome },
  { path: 'lotes', component: PublicacionLotesComponent},
];
