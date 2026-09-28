/**
 * Paleta para distinguir ONGs de un vistazo (chips del formulario de oferta y su resumen).
 * Es el único lugar a tocar para cambiar los colores: el estilo del chip está en `.chip-ong`
 * (styles.scss) y se deriva de este color.
 *
 * Base Okabe-Ito (distinguible con daltonismo) sin su amarillo ni su bermellón, que se
 * confundirían con los avisos y errores de la grilla, más dos tonos para llegar a 8.
 */
export const PALETA_ONG: readonly string[] = [
  '#0072B2', // azul
  '#E69F00', // naranja
  '#009E73', // verde azulado
  '#CC79A7', // violeta rosado
  '#56B4E9', // celeste
  '#7B5EA7', // violeta
  '#8C6D31', // marrón
  '#5A5A5A', // gris
];

/** Color estable por ONG (según su id): la misma ONG se ve igual en todas las pantallas. */
export function colorOng(ongId: number): string {
  return PALETA_ONG[Math.abs(ongId) % PALETA_ONG.length];
}
