import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { ROL_LABEL, Rol } from '../../core/models/rol';
import { Auth } from '../../core/services/auth';
import { Notifications } from '../notifications/notifications';

interface NavLink {
  label: string;
  path: string;
}

/** Links del menú por perfil. Agregá acá el link de cada pantalla nueva. */
const NAV_LINKS: Record<Rol, NavLink[]> = {
  MUNICIPAL: [
    { label: 'Inicio', path: '/municipal' },
    { label: 'Registrar Emergencia', path: '/municipal/emergencias/nueva' } // <-- Enlace agregado
  ],
  COORDINADOR: [{ label: 'Inicio', path: '/coordinador' }],
  ONG: [{ label: 'Inicio', path: '/ong' }],
  AUDITOR: [{ label: 'Inicio', path: '/auditor' }],
};

@Component({
  imports: [Notifications, RouterLink, RouterLinkActive],
  selector: 'app-navbar',
  styleUrl: './navbar.scss',
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

  protected cerrarMenuSiEsNecesario(panelAbierto: boolean): void {
    if (panelAbierto) {
      this.menuAbierto.set(false);
    }
  }

  protected salir(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigateByUrl('/login'),
      error: (error) => {
        console.error('No se pudo cerrar la sesión', error);
      },
    });
  }
}
