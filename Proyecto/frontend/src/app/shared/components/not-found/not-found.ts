import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Auth } from '../../../core/services/auth';

@Component({
  imports: [RouterLink],
  selector: 'app-not-found',
  template: `
    <div class="text-center py-5">
      <h1 class="display-4">404</h1>
      <p class="text-body-secondary">La página que buscás no existe.</p>
      <a class="btn btn-primary" [routerLink]="auth.homeUrl()">Volver al inicio</a>
    </div>
  `,
})
export class NotFound {
  protected readonly auth = inject(Auth);
}
