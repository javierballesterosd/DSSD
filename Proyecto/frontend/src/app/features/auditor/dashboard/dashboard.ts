import { Component, computed, inject, signal } from '@angular/core';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import {
  DashboardFiltros,
  DashboardRankingResponse,
  DashboardSeguimiento,
  DashboardTiempo,
  DashboardRankingEtapa,
} from '@core/models/dashboard';

import { NIVELES_GRAVEDAD, NIVEL_GRAVEDAD_LABEL, NivelGravedad } from '@core/models/emergencia';

import { DashboardService } from '@core/services/dashboard';

import {
  FiltrosFecha,
  RangoFechas,
} from '@shared/components/filtros-fecha/filtros-fecha';

import { TarjetaIndicador } from '@shared/components/tarjeta-indicador/tarjeta-indicador';
import { GraficoBarras } from '@shared/components/grafico-barras/grafico-barras';
import { GraficoDonut } from '@shared/components/grafico-donut/grafico-donut';
import { TiemposGestion } from '@shared/components/tiempos-gestion/tiempos-gestion';
import { TablaSeguimiento } from '@shared/components/tabla-seguimiento/tabla-seguimiento';

@Component({
  selector: 'app-dashboard',
  imports: [
    FiltrosFecha,
    TarjetaIndicador,
    GraficoBarras,
    GraficoDonut,
    TiemposGestion,
    TablaSeguimiento,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly dashboardService = inject(DashboardService);

  protected readonly hayErrores = computed(
    () => Object.keys(this.errores()).length > 0
  );
  // ============================================================
  // ESTADO GENERAL
  // ============================================================

  protected readonly cargando = signal(false);

  protected readonly filtros = signal<DashboardFiltros>({
    limite: 8,
  });

  protected readonly errores = signal<Record<string, string>>({});

  protected readonly municipios = signal<DashboardRankingResponse[]>([]);
  protected readonly ongs = signal<DashboardRankingResponse[]>([]);
  protected readonly recursos = signal<DashboardRankingResponse[]>([]);
  protected readonly errorCatalogos = signal(false);
  protected readonly nivelesGravedad = NIVELES_GRAVEDAD;
  protected readonly gravedadLabel = NIVEL_GRAVEDAD_LABEL;
  protected readonly filtrosProceso = signal<Pick<DashboardFiltros, 'municipioId' | 'gravedad' | 'recursoId' | 'ongId'>>({});

  private solicitudActual = 0;

  // ============================================================
  // ORGANIZACIONES
  // ============================================================

  protected readonly ongsPorOfertas = signal<DashboardRankingResponse[]>([]);

  protected readonly ongsPorRecursos = signal<DashboardRankingResponse[]>([]);

  // ============================================================
  // EMERGENCIAS
  // ============================================================

  protected readonly municipiosPorEmergencias = signal<DashboardRankingResponse[]>([]);

  // ============================================================
  // RECURSOS
  // ============================================================

  protected readonly recursosPorNecesidad = signal<DashboardRankingResponse[]>([]);

  protected readonly recursosPorCantidad = signal<DashboardRankingResponse[]>([]);

  // ============================================================
  // TIEMPOS DE GESTIÓN
  // ============================================================

  protected readonly tiemposEtapas = signal<DashboardRankingEtapa[]>([]);

  protected readonly tiempoValidacion = signal<DashboardTiempo | null>(null);



  protected readonly tiempoFinalizacion = signal<DashboardTiempo | null>(null);

  // ============================================================
  // SEGUIMIENTO
  // ============================================================

  protected readonly emergenciasSeguimiento = signal<DashboardSeguimiento[]>([]);

  // ============================================================
  // INDICADORES
  // ============================================================

  protected readonly cantidadONGsRanking = computed(
    () => this.ongsPorOfertas().length,
  );

  protected readonly cantidadMunicipiosRanking = computed(
    () => this.municipiosPorEmergencias().length,
  );

  protected readonly cantidadEmergenciasSeguimiento = computed(
    () => this.emergenciasSeguimiento().length,
  );

  protected readonly cantidadEtapasAnalizadas = computed(
    () => this.tiemposEtapas().length,
  );

  // ============================================================
  // INICIALIZACIÓN
  // ============================================================

  constructor() {
    // Sin límites ni fechas para incluir todos los elementos en los selectores.
    this.dashboardService.municipiosPorEmergencias().subscribe({
      next: (municipios) => this.municipios.set(municipios),
      error: () => this.errorCatalogos.set(true),
    });
    this.dashboardService.ongsPorOfertas().subscribe({
      next: (ongs) => this.ongs.set(ongs),
      error: () => this.errorCatalogos.set(true),
    });
    this.dashboardService.recursosPorCantidad().subscribe({
      next: (recursos) => this.recursos.set(recursos),
      error: () => this.errorCatalogos.set(true),
    });
    this.cargarDatos();
  }

  // ============================================================
  // FILTROS
  // ============================================================

  protected aplicarFiltros(rango: RangoFechas): void {
    this.filtros.update((actuales) => ({
      ...actuales,
      desde: rango.desde,
      hasta: rango.hasta,
    }));

    this.cargarDatos();
  }

  protected cambiarMunicipio(event: Event): void {
    const valor = (event.target as HTMLSelectElement).value;
    this.filtrosProceso.update((actuales) => ({ ...actuales, municipioId: valor ? Number(valor) : undefined }));
    this.cargarDatos();
  }

  protected cambiarGravedad(event: Event): void {
    const valor = (event.target as HTMLSelectElement).value;
    this.filtrosProceso.update((actuales) => ({ ...actuales, gravedad: valor ? valor as NivelGravedad : undefined }));
    this.cargarDatos();
  }

  protected cambiarRecurso(event: Event): void {
    const valor = (event.target as HTMLSelectElement).value;
    this.filtrosProceso.update((actuales) => ({ ...actuales, recursoId: valor ? Number(valor) : undefined }));
    this.cargarDatos();
  }

  protected cambiarOng(event: Event): void {
    const valor = (event.target as HTMLSelectElement).value;
    this.filtrosProceso.update((actuales) => ({ ...actuales, ongId: valor ? Number(valor) : undefined }));
    this.cargarDatos();
  }

  protected limpiarFiltrosProceso(): void {
    this.filtrosProceso.set({});
    this.cargarDatos();
  }

  protected limpiarFiltros(): void {
    this.filtrosProceso.set({});
    this.filtros.set({
      limite: 8,
    });

    this.cargarDatos();
  }

  // ============================================================
  // CONSULTAS
  // ============================================================

  protected cargarDatos(): void {
    const numeroSolicitud = ++this.solicitudActual;

    const filtrosActuales = this.filtros();
    const filtrosBonita = { ...filtrosActuales, ...this.filtrosProceso() };

    this.cargando.set(true);
    this.errores.set({});

    const registrarError = <T>(
      observable: Observable<T>,
      nombre: string,
      valorAlternativo: T,
    ): Observable<T> => {
      return observable.pipe(
        catchError(() => {
          if (numeroSolicitud === this.solicitudActual) {
            this.errores.update((actuales) => ({
              ...actuales,
              [nombre]: 'No fue posible cargar esta información.',
            }));
          }

          return of(valorAlternativo);
        }),
      );
    };

    forkJoin({
      // Organizaciones
      ongsPorOfertas: registrarError(
        this.dashboardService.ongsPorOfertas(filtrosActuales),
        'ongsPorOfertas',
        [] as DashboardRankingResponse[],
      ),

      ongsPorRecursos: registrarError(
        this.dashboardService.ongsPorRecursosAsignados(filtrosActuales),
        'ongsPorRecursos',
        [] as DashboardRankingResponse[],
      ),

      // Emergencias
      municipiosPorEmergencias: registrarError(
        this.dashboardService.municipiosPorEmergencias(filtrosActuales),
        'municipiosPorEmergencias',
        [] as DashboardRankingResponse[],
      ),

      // Recursos
      recursosPorNecesidad: registrarError(
        this.dashboardService.recursosPorNecesidad(filtrosActuales),
        'recursosPorNecesidad',
        [] as DashboardRankingResponse[],
      ),

      recursosPorCantidad: registrarError(
        this.dashboardService.recursosPorCantidad(8),
        'recursosPorCantidad',
        [] as DashboardRankingResponse[],
      ),

      // Tiempos
      tiemposEtapas: registrarError(
        this.dashboardService.etapas(filtrosBonita),
        'tiemposEtapas',
        [] as DashboardRankingEtapa[],
      ),

      tiempoValidacion: registrarError(
        this.dashboardService.tiempoValidacionNacional(filtrosBonita),
        'tiempoValidacion',
        this.tiempoVacio('Validación nacional'),
      ),



      tiempoFinalizacion: registrarError(
        this.dashboardService.tiempoEmergenciasFinalizadas(filtrosBonita),
        'tiempoFinalizacion',
        this.tiempoVacio('Finalización de emergencias'),
      ),

      // Seguimiento
      emergenciasSeguimiento: registrarError(
        this.dashboardService.seguimiento(filtrosBonita),
        'emergenciasSeguimiento',
        [] as DashboardSeguimiento[],
      ),
    }).subscribe({
      next: (resultado) => {
        // Evita que una consulta anterior sobrescriba una más reciente.
        if (numeroSolicitud !== this.solicitudActual) {
          return;
        }

        this.ongsPorOfertas.set(resultado.ongsPorOfertas);
        this.ongsPorRecursos.set(resultado.ongsPorRecursos);

        this.municipiosPorEmergencias.set(resultado.municipiosPorEmergencias);

        this.recursosPorNecesidad.set(resultado.recursosPorNecesidad);
        this.recursosPorCantidad.set(resultado.recursosPorCantidad);

        this.tiemposEtapas.set(resultado.tiemposEtapas);

        this.tiempoValidacion.set(resultado.tiempoValidacion);
        this.tiempoFinalizacion.set(resultado.tiempoFinalizacion);

        this.emergenciasSeguimiento.set(resultado.emergenciasSeguimiento);

        this.cargando.set(false);
      },

      error: () => {
        if (numeroSolicitud === this.solicitudActual) {
          this.cargando.set(false);
        }
      },
    });
  }

  // ============================================================
  // UTILIDADES
  // ============================================================

  private tiempoVacio(etapa: string): DashboardTiempo {
    return {
      etapa,
      cantidad: 0,
      promedioSegundos: null,
    };
  }

  protected errorDe(clave: string): string | null {
    return this.errores()[clave] ?? null;
  }

  protected formatearDuracion(segundos: number | null | undefined): string {
    if (segundos == null) {
      return 'Sin datos';
    }

    if (segundos < 0) {
      return 'Dato inválido';
    }

    if (segundos > 0 && segundos < 1) {
      return 'Menos de 1 seg';
    }

    const dias = Math.floor(segundos / 86400);
    const horas = Math.floor((segundos % 86400) / 3600);
    const minutos = Math.floor((segundos % 3600) / 60);

    if (dias > 0) {
      return `${dias} d ${horas} h`;
    }

    if (horas > 0) {
      return `${horas} h ${minutos} min`;
    }

    if (minutos > 0) {
      return `${minutos} min`;
    }

    return `${Math.floor(segundos)} seg`;
  }
}
