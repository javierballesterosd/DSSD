/** Formatos del `date` pipe. Requieren el locale `es` (ver app.config.ts) para el nombre del mes. */

/** 28 de septiembre de 2026, 23:30hs */
export const FECHA_LARGA = "d 'de' MMMM 'de' y, HH:mm'hs'";

/** 09/28/26, 21:13hs (para listados y tablas) */
export const FECHA_LISTADO = "MM/dd/yy, HH:mm'hs'";

/** 28 de septiembre de 2026 (sin hora) */
export const FECHA_DIA = "d 'de' MMMM 'de' y";

/** La próxima hora en punto: a las 12:47 devuelve las 13:00 (y a las 12:00, también las 13:00). */
export function proximaHoraEnPunto(desde: Date = new Date()): Date {
  const fecha = new Date(desde);
  fecha.setMinutes(0, 0, 0);
  fecha.setHours(fecha.getHours() + 1);
  return fecha;
}

/** Suma días a una fecha conservando la hora. */
export function sumarDias(fecha: Date, dias: number): Date {
  const resultado = new Date(fecha);
  resultado.setDate(resultado.getDate() + dias);
  return resultado;
}

/** Valor para un `<input type="datetime-local">` (yyyy-MM-ddTHH:mm, en hora local). */
export function aDatetimeLocal(fecha: Date): string {
  const dos = (n: number) => String(n).padStart(2, '0');
  return (
    `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}` +
    `T${dos(fecha.getHours())}:${dos(fecha.getMinutes())}`
  );
}
