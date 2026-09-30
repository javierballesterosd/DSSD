import { PALETA_ONG, colorOng } from './ong-colores';

describe('colorOng', () => {
  it('is stable for the same ONG', () => {
    expect(colorOng(3)).toBe(colorOng(3));
  });

  it('does not repeat colors within the palette size', () => {
    const colores = Array.from({ length: PALETA_ONG.length }, (_, i) => colorOng(i + 1));
    expect(new Set(colores).size).toBe(PALETA_ONG.length);
  });
});
