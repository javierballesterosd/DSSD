import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { ROL_LABEL, Rol } from '../../core/models/rol';
import { Auth } from '../../core/services/auth';

interface NavLink {
  label: string;
  path: string;
}

/** Links del menú por perfil. Agregá acá el link de cada pantalla nueva. */
const NAV_LINKS: Record<Rol, NavLink[]> = {
  MUNICIPAL: [{ label: 'Inicio', path: '/municipal' }],
  COORDINADOR: [{ label: 'Inicio', path: '/coordinador' }],
  ONG: [{ label: 'Inicio', path: '/ong' }],
  AUDITOR: [{ label: 'Inicio', path: '/auditor' }],
};

@Component({
  imports: [RouterLink, RouterLinkActive],
  selector: 'app-navbar',
  templateUrl: './navbar.html',
})
export class Navbar {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  protected readonly usuario = this.auth.usuario;
  protected readonly homeUrl = this.auth.homeUrl;
  protected readonly rolLabel = ROL_LABEL;
  protected readonly links = computed(() => {
    const rol = this.auth.rol();
    return rol ? NAV_LINKS[rol] : [];
  });
  protected readonly menuAbierto = signal(false);

  protected salir(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
