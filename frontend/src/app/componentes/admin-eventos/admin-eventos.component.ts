import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CourtResponse } from '../../modelos/court.model';
import { CreateEventRequest, EventCategory, EventRegistration, EventResponse } from '../../modelos/event.models';
import { CourtApiService } from '../../servicios/court-api.service';
import { EventsApiService } from '../../servicios/events-api.service';
import { ScheduleApiService } from '../../servicios/schedule-api.service';

@Component({
  selector: 'app-admin-eventos',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './admin-eventos.component.html',
  styleUrl: './admin-eventos.component.css',
})
export class AdminEventosComponent implements OnInit {
  private readonly eventsApi = inject(EventsApiService);
  private readonly courtApi = inject(CourtApiService);
  private readonly scheduleApi = inject(ScheduleApiService);

  events = signal<EventResponse[]>([]);
  allCourts = signal<CourtResponse[]>([]);
  scheduleSlots = signal<string[]>([]);
  expandedEventId = signal<string | null>(null);
  registrations = signal<EventRegistration[]>([]);
  loadingRegistrations = signal(false);
  confirmDeleteEventId = signal<string | null>(null);
  confirmDeleteRegId = signal<string | null>(null);

  createSuccess = signal('');
  createError = signal('');
  actionError = signal('');

  form: CreateEventRequest = {
    title: '',
    description: '',
    eventDate: '',
    startTime: '',
    endTime: '',
    maxCapacity: 16,
    category: 'TORNEO',
    sport: undefined,
    courtIds: [],
  };

  selectedSport = signal<'FUTBOL' | 'PADEL' | ''>('');
  courtsForSport = computed(() => {
    const sport = this.selectedSport();
    if (!sport) return [];
    return this.allCourts().filter(c => c.sport === sport);
  });

  selectedCourtIds = signal<string[]>([]);

  readonly categories: EventCategory[] = ['TORNEO', 'LIGA', 'MASTERCLASS', 'SOCIAL'];

  ngOnInit() {
    this.loadEvents();
    this.loadCourts();
    this.loadSlots();
  }

  private loadEvents() {
    this.eventsApi.getAllEvents().subscribe({
      next: (data) => this.events.set(data),
    });
  }

  private loadCourts() {
    this.courtApi.getCourts().subscribe({
      next: (courts) => this.allCourts.set(courts),
    });
  }

  private loadSlots() {
    this.scheduleApi.getSlots().subscribe({
      next: (slots) => this.scheduleSlots.set(slots),
    });
  }

  onSportChange(sport: string) {
    this.selectedSport.set(sport as 'FUTBOL' | 'PADEL' | '');
    this.selectedCourtIds.set([]);
    this.form.sport = sport as any || undefined;
    this.form.courtIds = [];
  }

  toggleCourt(id: string) {
    const current = this.selectedCourtIds();
    if (current.includes(id)) {
      this.selectedCourtIds.set(current.filter(c => c !== id));
    } else {
      this.selectedCourtIds.set([...current, id]);
    }
    this.form.courtIds = this.selectedCourtIds();
  }

  isCourtSelected(id: string): boolean {
    return this.selectedCourtIds().includes(id);
  }

  submitCreate(event: Event) {
    event.preventDefault();
    this.createError.set('');
    this.createSuccess.set('');

    const payload: CreateEventRequest = {
      ...this.form,
      startTime: this.form.startTime ? `${this.form.startTime}:00` : '',
      endTime: this.form.endTime ? `${this.form.endTime}:00` : '',
      sport: this.form.sport || undefined,
      courtIds: this.selectedCourtIds().length ? this.selectedCourtIds() : undefined,
    };

    this.eventsApi.createEvent(payload).subscribe({
      next: () => {
        this.createSuccess.set('Evento creado correctamente.');
        this.resetForm();
        this.loadEvents();
        setTimeout(() => this.createSuccess.set(''), 4000);
      },
      error: (err) => {
        this.createError.set(err?.error?.message || 'Error al crear el evento');
      },
    });
  }

  private resetForm() {
    this.form = {
      title: '',
      description: '',
      eventDate: '',
      startTime: '',
      endTime: '',
      maxCapacity: 16,
      category: 'TORNEO',
      sport: undefined,
      courtIds: [],
    };
    this.selectedSport.set('');
    this.selectedCourtIds.set([]);
  }

  toggleExpand(eventId: string) {
    if (this.expandedEventId() === eventId) {
      this.expandedEventId.set(null);
      this.registrations.set([]);
    } else {
      this.expandedEventId.set(eventId);
      this.loadRegistrations(eventId);
    }
    this.confirmDeleteEventId.set(null);
    this.confirmDeleteRegId.set(null);
  }

  private loadRegistrations(eventId: string) {
    this.loadingRegistrations.set(true);
    this.eventsApi.getRegistrations(eventId).subscribe({
      next: (data) => {
        this.registrations.set(data);
        this.loadingRegistrations.set(false);
      },
      error: () => this.loadingRegistrations.set(false),
    });
  }

  requestDeleteEvent(id: string) {
    this.confirmDeleteEventId.set(id);
  }

  cancelDeleteEvent() {
    this.confirmDeleteEventId.set(null);
  }

  confirmDeleteEvent(id: string) {
    this.eventsApi.deleteEvent(id).subscribe({
      next: () => {
        this.confirmDeleteEventId.set(null);
        this.expandedEventId.set(null);
        this.registrations.set([]);
        this.loadEvents();
      },
      error: (err) => this.actionError.set(err?.error?.message || 'Error al eliminar evento'),
    });
  }

  requestDeleteReg(regId: string) {
    this.confirmDeleteRegId.set(regId);
  }

  cancelDeleteReg() {
    this.confirmDeleteRegId.set(null);
  }

  confirmDeleteReg(eventId: string, regId: string) {
    this.eventsApi.removeRegistration(eventId, regId).subscribe({
      next: () => {
        this.confirmDeleteRegId.set(null);
        this.loadRegistrations(eventId);
        this.loadEvents();
      },
      error: (err) => this.actionError.set(err?.error?.message || 'Error al eliminar inscripción'),
    });
  }

  spotsLeft(event: EventResponse): number {
    return event.maxCapacity - event.registeredCount;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const [year, month, day] = dateStr.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    return date.toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  categoryLabel(cat: EventCategory): string {
    const map: Record<EventCategory, string> = {
      TORNEO: 'Torneo',
      LIGA: 'Liga',
      MASTERCLASS: 'Masterclass',
      SOCIAL: 'Social',
    };
    return map[cat] ?? cat;
  }

  formatDateTime(iso: string): string {
    if (!iso) return '';
    const date = new Date(iso);
    return date.toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' })
      + ' · ' + date.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
  }
}
