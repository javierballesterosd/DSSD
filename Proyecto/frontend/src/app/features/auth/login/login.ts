import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ROLES, ROL_LABEL, Rol } from '../../../core/models/rol';
import { Auth } from '../../../core/services/auth';
import { mensajeDeError } from '../../../core/services/errores';
import { ToastService } from '../../../core/services/toast';

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-login',
  templateUrl: './login.html',
})
export class Login {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly cargando = signal(false);
  protected readonly mostrarPassword = signal(false);
  protected readonly passwordTieneTexto = signal(false);

  protected readonly roles = ROLES;
  protected readonly rolLabel = ROL_LABEL;

  protected readonly form = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  protected ingresar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.cargando()) {
      return;
    }

    this.cargando.set(true);
    this.form.disable();

    this.auth
      .login(this.form.getRawValue())
      .pipe(
        finalize(() => {
          this.cargando.set(false);
          this.form.enable();
        }),
      )
      .subscribe({
        next: () => this.router.navigateByUrl(this.auth.homeUrl()),
        error: (error: HttpErrorResponse) =>
          this.toast.error('No se pudo iniciar sesión', this.motivoDelError(error)),
      });
  }

  protected actualizarEstadoPassword(): void {
    const tieneTexto = this.form.controls.password.value.length > 0;
    this.passwordTieneTexto.set(tieneTexto);

    if (!tieneTexto) {
      this.mostrarPassword.set(false);
    }
  }

  protected alternarVisibilidadPassword(): void {
    this.mostrarPassword.update((visible) => !visible);
  }

  private motivoDelError(error: HttpErrorResponse): string {
    switch (error.status) {
      case 401:
        return 'Usuario o contraseña incorrectos.';
      case 404:
        return mensajeDeError(error, 'Tu usuario no está asociado a una ONG registrada.');
      default:
        return mensajeDeError(error, 'Ocurrió un error inesperado. Intentá de nuevo.');
    }
  }
}
