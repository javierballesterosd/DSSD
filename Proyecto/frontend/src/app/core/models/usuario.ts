import { Rol } from './rol';

/** Cada usuario tiene un único rol. */
export interface Usuario {
  userId: string;
  username: string;
  firstName: string;
  lastName: string;
  role: Rol;
  group: string;
}
