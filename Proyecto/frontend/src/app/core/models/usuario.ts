import { Rol } from './rol';

/** Cada usuario tiene un único rol. */
export interface Usuario {
  username: string;
  rol: Rol;
}
