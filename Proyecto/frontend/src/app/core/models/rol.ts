export const ROLES = ['MUNICIPAL', 'COORDINADOR', 'ONG', 'AUDITOR'] as const;

export type Rol = (typeof ROLES)[number];

export const ROL_LABEL: Record<Rol, string> = {
  MUNICIPAL: 'Operador Municipal',
  COORDINADOR: 'Centro Coordinador Regional',
  ONG: 'Representante de ONG',
  AUDITOR: 'Auditor / Directivo',
};

/** Ruta de inicio de cada perfil (coincide con el path de su feature en app.routes.ts). */
export const ROL_HOME: Record<Rol, string> = {
  MUNICIPAL: '/municipal',
  COORDINADOR: '/coordinador',
  ONG: '/ong',
  AUDITOR: '/auditor',
};
