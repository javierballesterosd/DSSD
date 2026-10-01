import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EmergenciaParaLoteResponse } from '../../../core/models/emergencia';
import { Auth } from '../../../core/services/auth';
import { Emergencias } from '../../../core/services/emergencias';
import { EmergenciaTarjeta } from '../../../shared/components/emergencia-tarjeta/emergencia-tarjeta';

/** Una emergencia se puede desglosar si no tiene lote o si el último fue cancelado. */
export function puedeDesglosar(emergencia: EmergenciaParaLoteResponse): boolean {
  return emergencia.estadoLote === null || emergencia.estadoLote === 'CANCELADO';
}

/** Inicio del coordinador: las emergencias de los municipios de su región. */
@Component({
  imports: [RouterLink, EmergenciaTarjeta],
  selector: 'app-emergencias-region',
  templateUrl: './emergencias-region.html',
})
export class EmergenciasRegion implements OnInit {
  private readonly emergenciasService = inject(Emergencias);
  private readonly auth = inject(Auth);

  protected readonly region = this.auth.usuario()?.regionNombre ?? null;
  protected readonly puedeDesglosar = puedeDesglosar;

  protected readonly emergencias = signal<EmergenciaParaLoteResponse[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);
  protected readonly paginaActual = signal(0);
  protected readonly totalPaginas = signal(0);

  private readonly tamanioPagina = 9;

  ngOnInit(): void {
    this.cargar(0);
  }

  /** Sin lote vigente lleva al formulario de publicación; con lote, a su detalle. */
  protected enlace(emergencia: EmergenciaParaLoteResponse): unknown[] {
    return puedeDesglosar(emergencia)
      ? ['/coordinador/emergencias', emergencia.id, 'lote', 'nuevo']
      : ['/coordinador/lotes', emergencia.loteId];
  }

  protected paginaAnterior(): void {
    if (this.paginaActual() > 0) {
      this.cargar(this.paginaActual() - 1);
    }
  }

  protected paginaSiguiente(): void {
    if (this.paginaActual() < this.totalPaginas() - 1) {
      this.cargar(this.paginaActual() + 1);
    }
  }

  private cargar(pagina: number): void {
    this.cargando.set(true);
    this.error.set(false);
    this.emergenciasService.listarParaLotes(pagina, this.tamanioPagina).subscribe({
      next: (respuesta) => {
        this.emergencias.set(respuesta.content);
        this.paginaActual.set(respuesta.number);
        this.totalPaginas.set(respuesta.totalPages);
        this.cargando.set(false);
      },
      error: () => {
        this.emergencias.set([]);
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }
}
