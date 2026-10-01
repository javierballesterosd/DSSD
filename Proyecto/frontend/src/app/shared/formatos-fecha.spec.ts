import { aDatetimeLocal, proximaHoraEnPunto, sumarDias } from './formatos-fecha';

describe('formatos-fecha', () => {
  it('proximaHoraEnPunto redondea a la hora siguiente', () => {
    const resultado = proximaHoraEnPunto(new Date(2026, 9, 1, 12, 47, 30));

    expect(aDatetimeLocal(resultado)).toBe('2026-10-01T13:00');
  });

  it('proximaHoraEnPunto pasa a la hora siguiente aunque ya sea en punto', () => {
    const resultado = proximaHoraEnPunto(new Date(2026, 9, 1, 12, 0, 0));

    expect(aDatetimeLocal(resultado)).toBe('2026-10-01T13:00');
  });

  it('proximaHoraEnPunto cruza al día siguiente', () => {
    const resultado = proximaHoraEnPunto(new Date(2026, 9, 31, 23, 15));

    expect(aDatetimeLocal(resultado)).toBe('2026-11-01T00:00');
  });

  it('sumarDias conserva la hora', () => {
    const resultado = sumarDias(new Date(2026, 9, 1, 13, 0), 2);

    expect(aDatetimeLocal(resultado)).toBe('2026-10-03T13:00');
  });
});
