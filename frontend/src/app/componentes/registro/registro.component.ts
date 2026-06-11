import {HttpErrorResponse} from '@angular/common/http';
import {Component, inject, signal} from '@angular/core';
import {FormsModule, NgForm} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';

import {AuthStore} from '../../auth.store';
import {AuthApiService} from '../../servicios/auth-api.service';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './registro.component.html',
  styleUrl: './registro.component.css',
})
export class RegistroComponent {
  readonly minPasswordLength = 8;
  readonly maxPasswordLength = 128;
  readonly maxNameLength = 120;
  readonly maxEmailLength = 180;
  readonly phonePattern = '^[+]?[0-9\\s\\-().]{7,20}$';

  private authApi = inject(AuthApiService);
  private authStore = inject(AuthStore);
  private router = inject(Router);

  name = '';
  email = '';
  password = '';
  showPassword = false;
  phone = '';
  birthDate = '';
  errorMessage = signal('');
  loading = signal(false);

  get maxBirthDate(): string {
    
    const d = new Date();
    d.setFullYear(d.getFullYear() - 14);
    return d.toISOString().split('T')[0];
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  submitRegister(form: NgForm) {
    if (this.loading() || form.invalid) {
      return;
    }

    if (this.authStore.isLoggedIn()) {
      this.router.navigate(['/']);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.authApi.register(
      this.name.trim(),
      this.email.trim().toLowerCase(),
      this.password,
      this.phone.trim(),
      this.birthDate
    ).subscribe({
      next: (session) => {
        this.authStore.setSession(session);
        this.loading.set(false);
        this.router.navigate(['/'], { replaceUrl: true });
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        if (error.status === 0) {
          this.errorMessage.set('No se pudo conectar con el servidor.');
        } else if (error.error?.message) {
          this.errorMessage.set(error.error.message);
        } else {
          this.errorMessage.set('No se pudo crear la cuenta. Es posible que el email ya esté registrado.');
        }
      },
    });
  }
}
