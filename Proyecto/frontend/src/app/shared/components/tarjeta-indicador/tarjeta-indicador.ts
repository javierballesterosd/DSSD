import { Component, input } from '@angular/core';

@Component({
  selector: 'app-tarjeta-indicador',
  templateUrl: './tarjeta-indicador.html',
  styleUrl: './tarjeta-indicador.scss',
})
export class TarjetaIndicador {
  readonly titulo = input.required<string>();
  readonly valor = input.required<string | number>();
  readonly descripcion = input<string>('');
  readonly icono = input.required<string>();
  readonly color = input<string>('primary');
}
