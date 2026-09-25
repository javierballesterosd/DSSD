import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ROLES, ROL_LABEL, Rol } from '../../../core/models/rol';
import { Auth } from '../../../core/services/auth';

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-login',
  templateUrl: './login.html',
})
export class Login {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  protected readonly roles = ROLES;
  protected readonly rolLabel = ROL_LABEL;

  protected readonly form = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] })
  });

  protected ingresar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => this.router.navigateByUrl(this.auth.homeUrl()),
      error: (error) => {
        console.log(error); //ESTO SE PUEDE AGREGAR UN COMPONENTE DE ERROR PARA MOSTRARLO EN PANTALLA
      }
    });
  }
}
