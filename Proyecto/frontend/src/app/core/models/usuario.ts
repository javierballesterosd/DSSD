import { Rol } from './rol';

/** Cada usuario tiene un único rol. */
export interface Usuario {
  userId: string;
  username: string;
  firstName: string;
  lastName: string;
  role: Rol;
  group: string;
  /** Solo para el rol ONG: ONG a la que pertenece el representante. */
  ongId?: number | null;
  ongNombre?: string | null;
  /** Solo para el rol MUNICIPAL: municipio del operador. */
  municipioId?: number | null;
  municipioNombre?: string | null;
  /** Para MUNICIPAL (región de su municipio) y COORDINADOR (su región). */
  regionId?: number | null;
  regionNombre?: string | null;
}
