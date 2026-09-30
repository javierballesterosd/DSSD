import { normalizar } from './ong-selector';

describe('normalizar', () => {
  it('ignora mayúsculas, acentos y espacios de los bordes', () => {
    expect(normalizar('  Cáritas Arquidiócesis ')).toBe('caritas arquidiocesis');
  });

  it('permite buscar sin acentos dentro de un nombre acentuado', () => {
    expect(
      normalizar('Cáritas Arquidiócesis de Buenos Aires').includes(normalizar('CARITAS')),
    ).toBe(true);
  });
});
