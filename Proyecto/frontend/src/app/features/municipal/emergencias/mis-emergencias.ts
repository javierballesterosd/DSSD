import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EmergenciaParaLoteResponse } from '../../../core/models/emergencia';
import { Auth } from '../../../core/services/auth';
import { Emergencias } from '../../../core/services/emergencias';
import { EmergenciaTarjeta } from '../../../shared/components/emergencia-tarjeta/emergencia-tarjeta';

/** Inicio del operador municipal: las emergencias registradas por su municipio. */
@Component({
  imports: [RouterLink, EmergenciaTarjeta],
  selector: 'app-mis-emergencias',
  templateUrl: './mis-emergencias.html',
})
export class MisEmergencias implements OnInit {
  private readonly emergenciasService = inject(Emergencias);
  private readonly auth = inject(Auth);

  protected readonly municipio = this.auth.usuario()?.municipioNombre ?? null;

  protected readonly emergencias = signal<EmergenciaParaLoteResponse[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal(false);

  ngOnInit(): void {
    this.emergenciasService.listarMias().subscribe({
      next: (emergencias) => {
        this.emergencias.set(emergencias);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }
}
