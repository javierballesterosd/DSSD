import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Auth } from '../../../core/services/auth';
import { LoteDetalle } from './lote-detalle';

const LOTE = {
  id: 10,
  titulo: 'Asistencia alimentaria',
  estado: 'ACTIVO',
  convocatoriaAbierta: true,
  fechaAperturaOfertas: null,
  fechaCierreOfertas: null,
  emergencia: {
    id: 1,
    zonaAfectada: 'Zona Norte',
    nivelGravedad: 'ALTA',
    nivelGravedadEtiqueta: 'Alta',
    descripcion: 'Inundación',
    fechaRegistro: '2026-09-20T10:00:00',
    municipio: 'La Plata',
  },
  items: [],
};

const OFERTA = {
  id: 7,
  estado: 'PENDIENTE',
  estadoEtiqueta: 'Pendiente',
  fechaOferta: '2026-09-27T10:00:00',
  loteId: 10,
  loteTitulo: 'Asistencia alimentaria',
  emergenciaZona: 'Zona Norte',
  ongs: [
    { id: 1, razonSocial: 'Cruz Roja La Plata' },
    { id: 2, razonSocial: 'Cáritas' },
  ],
  aportes: [
    {
      itemLoteId: 200,
      recursoNombre: 'Raciones',
      unidadMedida: 'raciones',
      cantidadRequerida: 1000,
      totalOfrecido: 600,
      porOng: [
        { ongId: 1, razonSocial: 'Cruz Roja La Plata', cantidadOfrecida: 400 },
        { ongId: 2, razonSocial: 'Cáritas', cantidadOfrecida: 200 },
      ],
    },
  ],
};

describe('LoteDetalle (mis ofertas)', () => {
  let http: HttpTestingController;

  async function crear(ongId: number | null) {
    TestBed.configureTestingModule({
      imports: [LoteDetalle],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '10' }) } },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(Auth).login({ username: 'u', password: 'bpm' }).subscribe();
    http
      .expectOne((r) => r.url.endsWith('/auth/login'))
      .flush({
        userId: '1',
        username: 'u',
        firstName: 'U',
        lastName: 'U',
        role: ongId === null ? 'COORDINADOR' : 'ONG',
        group: 'G',
        ongId,
      });
    const fixture = TestBed.createComponent(LoteDetalle);
    await fixture.whenStable();
    return fixture;
  }

  afterEach(() => http.verify());

  it('lists the offers of the user ONG and opens the detail', async () => {
    const fixture = await crear(1);
    http.expectOne((r) => r.url.endsWith('/lotes/10')).flush(LOTE);
    http
      .expectOne((r) => r.url.endsWith('/ofertas/mias') && r.params.get('loteId') === '10')
      .flush([OFERTA]);
    await fixture.whenStable();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Mis ofertas para este lote');
    expect(el.textContent).toContain('Pendiente');

    el.querySelector<HTMLButtonElement>('button.btn-outline-primary')!.click();
    await fixture.whenStable();

    expect(el.querySelector('app-oferta-detalle-modal')).not.toBeNull();
    expect(el.textContent).toContain('Oferta #7');
    expect(el.textContent).toContain('400 raciones');
  });

  it('says so when the ONG has no offers yet', async () => {
    const fixture = await crear(1);
    http.expectOne((r) => r.url.endsWith('/lotes/10')).flush(LOTE);
    http.expectOne((r) => r.url.endsWith('/ofertas/mias')).flush([]);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Tu ONG todavía no ofertó para este lote',
    );
  });

  it('does not request offers when the user has no ONG', async () => {
    const fixture = await crear(null);
    http.expectOne((r) => r.url.endsWith('/lotes/10')).flush(LOTE);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Mis ofertas');
  });
});
