import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';

import { ClientsApiService } from '../../servicios/clients-api.service';
import { Client, CreateClientRequest } from '../../modelos/client.models';
import { DatePickerComponent } from '../date-picker/date-picker.component';
import { FlashMessageComponent } from '../flash-message/flash-message.component';

@Component({
  selector: 'app-admin-clientes',
  standalone: true,
  imports: [FormsModule, DatePickerComponent, FlashMessageComponent],
  templateUrl: './admin-clientes.component.html',
  styleUrl: './admin-clientes.component.css',
})
export class AdminClientesComponent implements OnInit {
  private clientsApi = inject(ClientsApiService);
  readonly resultLimit = 10;

  readonly todayIso = (() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  })();

  clients = signal<Client[]>([]);
  loading = signal(true);
  search = signal('');
  successMsg = signal('');
  errorMsg = signal('');
  emailServerError = signal('');
  phoneServerError = signal('');
  passwordServerError = signal('');
  generalServerError = signal('');
  confirmDeleteId = signal<string | null>(null);

  newClient = {
    name: '',
    email: '',
    password: '',
    phone: '',
    birthDate: '',
  };

  showPassword = false;

  ngOnInit() {
    this.loadClients();
  }

  private loadClients() {
    this.loading.set(true);
    this.clientsApi.getAll(this.search(), this.resultLimit).subscribe({
      next: (clients) => {
        this.clients.set(clients);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  applySearch(event: Event) {
    event.preventDefault();
    this.loadClients();
  }

  addClient(event: Event, form: NgForm) {
    event.preventDefault();
    this.successMsg.set('');
    this.clearServerErrors();

    const request: CreateClientRequest = {
      name: this.newClient.name.trim(),
      email: this.newClient.email.trim().toLowerCase(),
      password: this.newClient.password,
      phone: this.newClient.phone.trim(),
      birthDate: this.newClient.birthDate,
    };

    this.clientsApi.create(request).subscribe({
      next: () => {
        this.newClient = { name: '', email: '', password: '', phone: '', birthDate: '' };
        form.resetForm({ name: '', email: '', password: '', phone: '', birthDate: '' });
        this.successMsg.set('Socio registrado correctamente.');
        this.loadClients();
        setTimeout(() => this.successMsg.set(''), 4000);
      },
      error: (err) => {
        this.assignServerError(err?.error?.message || 'Error al registrar el socio.');
      }
    });
  }

  private clearServerErrors() {
    this.emailServerError.set('');
    this.phoneServerError.set('');
    this.passwordServerError.set('');
    this.generalServerError.set('');
  }

  private assignServerError(msg: string) {
    const lower = msg.toLowerCase();
    if (lower.includes('email') || lower.includes('correo')) {
      this.emailServerError.set(msg);
    } else if (lower.includes('teléfono') || lower.includes('telefono') || lower.includes('phone')) {
      this.phoneServerError.set(msg);
    } else if (lower.includes('contraseña') || lower.includes('password')) {
      this.passwordServerError.set(msg);
    } else {
      this.generalServerError.set(msg);
    }
  }

  requestDelete(id: string) {
    this.confirmDeleteId.set(id);
  }

  cancelDelete() {
    this.confirmDeleteId.set(null);
  }

  confirmDelete(id: string) {
    this.clientsApi.delete(id).subscribe({
      next: () => {
        this.confirmDeleteId.set(null);
        this.loadClients();
      },
    });
  }
}
