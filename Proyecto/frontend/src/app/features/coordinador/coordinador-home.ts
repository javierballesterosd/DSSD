import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-coordinador-home',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="container py-4">
      <h1 class="h3">Centro Coordinador Regional</h1>

      <p class="text-body-secondary">Inicio de este perfil.</p>

      <div class="mt-4">
        <a routerLink="/coordinador/lotes" class="btn btn-primary">
          <i class="bi bi-box-seam me-2"></i>
          Publicación de Lotes
        </a>
      </div>
    </div>
  `,
})
export class CoordinadorHome {}
