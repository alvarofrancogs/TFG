import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthStore } from '../../auth.store';
import { EventCategory, EventResponse } from '../../modelos/event.models';
import { EventsApiService } from '../../servicios/events-api.service';
import { NewsletterService } from '../../servicios/newsletter.service';

@Component({
  selector: 'app-eventos',
  imports: [RouterLink, FormsModule],
  templateUrl: './eventos.html',
  styleUrl: './eventos.css',
})
export class Eventos implements OnInit {
  private readonly eventsApi = inject(EventsApiService);
  private readonly authStore = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly newsletterService = inject(NewsletterService);

  readonly isLoggedIn = this.authStore.isLoggedIn;
  readonly session = this.authStore.session;

  events = signal<EventResponse[]>([]);
  activeFilter = signal<EventCategory | 'TODOS'>('TODOS');
  loading = signal(true);
  actionError = signal('');

  newsletterEmail = signal('');
  subscribeLoading = signal(false);
  subscribeSuccess = signal(false);
  subscribeError = signal('');

  filteredEvents = computed(() => {
    const filter = this.activeFilter();
    if (filter === 'TODOS') return this.events();
    return this.events().filter(e => e.category === filter);
  });

  ngOnInit() {
    this.eventsApi.getActiveEvents().subscribe({
      next: (data) => {
        this.events.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  setFilter(filter: EventCategory | 'TODOS') {
    this.activeFilter.set(filter);
  }

  register(eventId: string) {
    if (!this.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }
    this.actionError.set('');
    this.eventsApi.register(eventId).subscribe({
      next: () => this.refreshEvent(eventId, true),
      error: (err) => this.actionError.set(err?.error?.message || 'Error al inscribirse'),
    });
  }

  unregister(eventId: string) {
    this.actionError.set('');
    this.eventsApi.unregister(eventId).subscribe({
      next: () => this.refreshEvent(eventId, false),
      error: (err) => this.actionError.set(err?.error?.message || 'Error al des-inscribirse'),
    });
  }

  private refreshEvent(eventId: string, registered: boolean) {
    this.events.update(list =>
      list.map(e => {
        if (e.id !== eventId) return e;
        const delta = registered ? 1 : -1;
        return { ...e, isRegistered: registered, registeredCount: e.registeredCount + delta };
      })
    );
  }

  spotsLeft(event: EventResponse): number {
    return event.maxCapacity - event.registeredCount;
  }

  formatDate(dateStr: string): { day: string; month: string } {
    const [year, month, day] = dateStr.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    return {
      day: String(day).padStart(2, '0'),
      month: date.toLocaleString('es', { month: 'short' }).replace('.', ''),
    };
  }

  categoryLabel(category: EventCategory): string {
    const labels: Record<EventCategory, string> = {
      TORNEO: 'Torneo',
      LIGA: 'Liga',
      MASTERCLASS: 'Masterclass',
      SOCIAL: 'Social',
    };
    return labels[category] ?? category;
  }

  subscribe() {
    const email = this.isLoggedIn()
      ? (this.session()?.email ?? '')
      : this.newsletterEmail();

    if (!email) return;

    this.subscribeLoading.set(true);
    this.subscribeError.set('');
    this.subscribeSuccess.set(false);

    this.newsletterService.subscribe(email).subscribe({
      next: () => {
        this.subscribeLoading.set(false);
        this.subscribeSuccess.set(true);
        this.newsletterEmail.set('');
      },
      error: () => {
        this.subscribeLoading.set(false);
        this.subscribeError.set('Error al suscribirse. Inténtalo de nuevo.');
      },
    });
  }
}
