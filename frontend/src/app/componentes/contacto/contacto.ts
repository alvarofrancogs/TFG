import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

import { environment } from '../../../environments/environment';

import { DropdownComponent, DropdownOption } from '../dropdown/dropdown.component';
import { FlashMessageComponent } from '../flash-message/flash-message.component';

@Component({
  selector: 'app-contacto',
  standalone: true,
  imports: [RouterLink, FormsModule, DropdownComponent, FlashMessageComponent],
  templateUrl: './contacto.html',
  styleUrl: './contacto.css',
})
export class Contacto {
  private readonly http = inject(HttpClient);

  asuntoOptions: DropdownOption[] = [
    { label: 'Información sobre instalaciones', value: 'Información sobre instalaciones' },
    { label: 'Reservas y disponibilidad', value: 'Reservas y disponibilidad' },
    { label: 'Membresías y tarifas', value: 'Membresías y tarifas' },
    { label: 'Incidencia técnica', value: 'Incidencia técnica' },
    { label: 'Otro', value: 'Otro' }
  ];

  nombre = signal('');
  apellidos = signal('');
  email = signal('');
  asunto = signal('');
  mensaje = signal('');
  privacidad = signal(false);

  loading = signal(false);
  success = signal(false);
  error = signal('');

  get canSubmit(): boolean {
    return (
      this.nombre().trim().length > 0 &&
      this.apellidos().trim().length > 0 &&
      this.email().trim().length > 0 &&
      this.asunto().length > 0 &&
      this.mensaje().trim().length > 0 &&
      this.privacidad() &&
      !this.loading()
    );
  }

  submit() {
    if (!this.canSubmit) return;

    this.loading.set(true);
    this.error.set('');

    this.http.post<void>(`${environment.apiBaseUrl}/contact`, {
      nombre: this.nombre().trim(),
      apellidos: this.apellidos().trim(),
      email: this.email().trim(),
      asunto: this.asunto(),
      mensaje: this.mensaje().trim(),
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message || 'Error al enviar el mensaje. Inténtalo de nuevo.');
      },
    });
  }
}
