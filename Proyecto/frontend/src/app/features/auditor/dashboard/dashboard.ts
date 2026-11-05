import { Component, computed, inject, signal } from '@angular/core';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import {
  DashboardFiltros,
  DashboardRankingResponse,
  DashboardSeguimiento,
  DashboardTiempo,
} from '@core/models/dashboard';

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

  protected readonly tiemposEtapas = signal<DashboardTiempo[]>([]);

  protected readonly tiempoValidacion = signal<DashboardTiempo | null>(null);

  protected readonly tiempoDecision = signal<DashboardTiempo | null>(null);

  protected readonly tiempoCompromiso = signal<DashboardTiempo | null>(null);

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

  protected limpiarFiltros(): void {
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
        this.dashboardService.etapas(filtrosActuales),
        'tiemposEtapas',
        [] as DashboardTiempo[],
      ),

      tiempoValidacion: registrarError(
        this.dashboardService.tiempoValidacionNacional(filtrosActuales),
        'tiempoValidacion',
        this.tiempoVacio('Validación nacional'),
      ),

      tiempoDecision: registrarError(
        this.dashboardService.tiempoDecisionMunicipal(filtrosActuales),
        'tiempoDecision',
        this.tiempoVacio('Decisión municipal'),
      ),

      tiempoCompromiso: registrarError(
        this.dashboardService.tiempoCompromiso(filtrosActuales),
        'tiempoCompromiso',
        this.tiempoVacio('Registro de compromiso'),
      ),

      tiempoFinalizacion: registrarError(
        this.dashboardService.tiempoEmergenciasFinalizadas(filtrosActuales),
        'tiempoFinalizacion',
        this.tiempoVacio('Finalización de emergencias'),
      ),

      // Seguimiento
      emergenciasSeguimiento: registrarError(
        this.dashboardService.seguimiento(filtrosActuales),
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
        this.tiempoDecision.set(resultado.tiempoDecision);
        this.tiempoCompromiso.set(resultado.tiempoCompromiso);
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
