import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CourtResponse } from '../../modelos/court.model';
import { CreateMaintenanceBlockRequest, Reservation, SportType } from '../../modelos/reservation.models';
import { CourtApiService } from '../../servicios/court-api.service';
import { ReservationsApiService } from '../../servicios/reservations-api.service';
import { ScheduleApiService } from '../../servicios/schedule-api.service';

import { DropdownComponent, DropdownOption } from '../dropdown/dropdown.component';

@Component({
  selector: 'app-admin-reservas',
  standalone: true,
  imports: [FormsModule, DropdownComponent],
  templateUrl: './admin-reservas.component.html',
  styleUrl: './admin-reservas.component.css',
})
export class AdminReservasComponent implements OnInit {
  private reservationsApi = inject(ReservationsApiService);
  private courtApi = inject(CourtApiService);
  private scheduleApi = inject(ScheduleApiService);

  selectedDate = signal(this.today());
  filterSport = signal<'TODOS' | 'FUTBOL' | 'PADEL'>('TODOS');
  reservations = signal<Reservation[]>([]);
  allCourts = signal<CourtResponse[]>([]);
  loading = signal(true);

  expandedReservationId = signal<string | null>(null);
  confirmCancelId = signal<string | null>(null);

  scheduleSlots = signal<string[]>([]);
  newSlotTime = '';
  scheduleError = signal('');
  scheduleSuccess = signal('');

  readonly halfHourSlots: string[] = (() => {
    const slots: string[] = [];
    for (let h = 6; h <= 23; h++) {
      slots.push(`${String(h).padStart(2, '0')}:00`);
      if (h < 23) slots.push(`${String(h).padStart(2, '0')}:30`);
    }
    return slots;
  })();
  readonly halfHourDropdownOptions: DropdownOption[] = this.halfHourSlots.map(s => ({ label: s, value: s }));
  
  loadError = signal('');

  courtsForBlock = signal<CourtResponse[]>([]);
  blockForm = {
    sport: 'PADEL' as SportType,
    courtId: '',
    date: '',
    time: '',
  };
  blockSuccess = signal('');
  blockError = signal('');

  
  filteredCourts = computed(() => {
    const sport = this.filterSport();
    const courts = this.allCourts();
    if (sport === 'TODOS') return courts;
    return courts.filter(c => c.sport === sport);
  });

  ngOnInit() {
    this.loadAll();
  }

  private loadAll() {
    this.loadScheduleSlots();
    this.loadAllCourts();
    this.loadReservations();
    this.loadBlockCourts();
  }

  private loadAllCourts() {
    this.courtApi.getCourts().subscribe({
      next: (courts) => this.allCourts.set(courts),
      error: () => this.loadError.set('No se pudieron cargar las pistas.'),
    });
  }

  private loadReservations() {
    this.loading.set(true);
    this.reservationsApi.getAll({ date: this.selectedDate() }).subscribe({
      next: (res) => {
        this.reservations.set(res);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private loadScheduleSlots() {
    this.scheduleApi.getSlots().subscribe({
      next: (slots) => {
        this.scheduleSlots.set(slots);
        this.loadError.set('');
        if (!this.blockForm.time || !slots.includes(this.blockForm.time)) {
          this.blockForm.time = slots[0] ?? '';
        }
      },
      error: () => {
        this.scheduleSlots.set([]);
        this.loadError.set('No se pudieron cargar horarios. Inicia sesión de nuevo.');
      },
    });
  }

  private loadBlockCourts() {
    this.courtApi.getCourts(this.blockForm.sport).subscribe({
      next: (courts) => {
        this.courtsForBlock.set(courts);
        if (!courts.some(c => c.id === this.blockForm.courtId)) {
          this.blockForm.courtId = courts[0]?.id ?? '';
        }
      },
      error: () => {
        this.courtsForBlock.set([]);
        this.blockForm.courtId = '';
      },
    });
  }

  setSportFilter(sport: 'TODOS' | 'FUTBOL' | 'PADEL') {
    this.filterSport.set(sport);
    this.expandedReservationId.set(null);
    this.confirmCancelId.set(null);
  }

  changeDate(date: string) {
    this.selectedDate.set(date);
    this.expandedReservationId.set(null);
    this.confirmCancelId.set(null);
    this.loadReservations();
  }

  
  cellReservation(courtId: string, time: string): Reservation | null {
    return this.reservations().find(r =>
      r.court === this.allCourts().find(c => c.id === courtId)?.name
      && this.normalizeTime(r.time) === time
    ) ?? null;
  }

  
  expandedResForSlot(slot: string): Reservation | null {
    const expandedId = this.expandedReservationId();
    if (!expandedId) return null;
    const res = this.reservations().find(r => r.id === expandedId);
    if (!res) return null;
    return this.normalizeTime(res.time) === slot ? res : null;
  }

  
  normalizeTimePublic(time: string): string {
    return this.normalizeTime(time);
  }

  toggleExpand(reservationId: string) {
    if (this.expandedReservationId() === reservationId) {
      this.expandedReservationId.set(null);
      this.confirmCancelId.set(null);
    } else {
      this.expandedReservationId.set(reservationId);
      this.confirmCancelId.set(null);
    }
  }

  requestCancel(id: string) {
    this.confirmCancelId.set(id);
  }

  cancelConfirmation() {
    this.confirmCancelId.set(null);
  }

  cancelReservation(id: string) {
    this.reservationsApi.delete(id).subscribe({
      next: () => {
        this.confirmCancelId.set(null);
        this.expandedReservationId.set(null);
        this.loadReservations();
      },
    });
  }

  removeMaintenance(id: string) {
    this.reservationsApi.delete(id).subscribe({
      next: () => {
        this.expandedReservationId.set(null);
        this.loadReservations();
      },
    });
  }

  addSlot() {
    if (!this.newSlotTime) return;
    const parts = this.newSlotTime.split(':');
    const minutes = parseInt(parts[1] ?? '0', 10);
    if (minutes !== 0 && minutes !== 30) {
      this.scheduleError.set('Solo se permiten franjas en punto (:00) o en media hora (:30).');
      return;
    }
    this.scheduleError.set('');
    this.scheduleSuccess.set('');
    this.scheduleApi.addSlot(this.newSlotTime).subscribe({
      next: () => {
        this.scheduleSuccess.set(`Franja ${this.newSlotTime} activada`);
        this.newSlotTime = '';
        this.loadScheduleSlots();
        this.loadReservations();
        setTimeout(() => this.scheduleSuccess.set(''), 3000);
      },
      error: (err) => {
        const msg = err?.error?.message || err?.error || 'Error al activar franja';
        this.scheduleError.set(typeof msg === 'string' ? msg : 'Error al activar franja');
      },
    });
  }

  removeSlot(slot: string) {
    this.scheduleError.set('');
    this.scheduleSuccess.set('');
    this.scheduleApi.removeSlot(slot).subscribe({
      next: () => {
        this.scheduleSuccess.set(`Franja ${slot} desactivada`);
        this.loadScheduleSlots();
        this.loadReservations();
        setTimeout(() => this.scheduleSuccess.set(''), 3000);
      },
      error: (err) => {
        const msg = err?.error?.message || err?.error || 'Error al desactivar franja';
        this.scheduleError.set(typeof msg === 'string' ? msg : 'Error al desactivar franja');
      },
    });
  }

  onBlockSportChange(sport: string) {
    this.blockForm.sport = sport as SportType;
    this.blockForm.courtId = '';
    this.loadBlockCourts();
  }

  blockCourt(event: Event) {
    event.preventDefault();
    if (!this.blockForm.date || !this.blockForm.time || !this.blockForm.courtId) return;

    this.blockError.set('');
    this.blockSuccess.set('');

    const request: CreateMaintenanceBlockRequest = {
      sport: this.blockForm.sport,
      courtId: this.blockForm.courtId,
      date: this.blockForm.date,
      time: `${this.blockForm.time}:00`,
    };

    this.reservationsApi.createMaintenance(request).subscribe({
      next: () => {
        this.blockForm.date = '';
        this.blockSuccess.set('Pista bloqueada correctamente.');
        this.loadReservations();
        setTimeout(() => this.blockSuccess.set(''), 4000);
      },
      error: (err) => {
        const msg = err?.error?.message || err?.error || 'Error al bloquear pista';
        this.blockError.set(typeof msg === 'string' ? msg : 'Error al bloquear pista');
      }
    });
  }

  statusLabel(status: string) {
    if (status === 'PENDING') return 'Pendiente';
    if (status === 'CONFIRMED') return 'Confirmada';
    if (status === 'COMPLETED') return 'Completada';
    if (status === 'MAINTENANCE') return 'Mantenimiento';
    return status;
  }

  
  slotRange(slot: string): { start: string; end: string } {
    const slots = this.scheduleSlots();
    const idx = slots.indexOf(slot);
    const next = slots[idx + 1];
    if (next) {
      return { start: slot, end: next };
    }
    const prev = slots[idx - 1];
    const durationMs = prev ? this.timeDiffMs(prev, slot) : 90 * 60 * 1000;
    const endMs = this.timeToMs(slot) + durationMs;
    return { start: slot, end: this.msToTime(endMs) };
  }

  private timeToMs(time: string): number {
    const [h, m] = time.split(':').map(Number);
    return (h * 60 + m) * 60 * 1000;
  }

  private timeDiffMs(from: string, to: string): number {
    return this.timeToMs(to) - this.timeToMs(from);
  }

  private msToTime(ms: number): string {
    const totalMin = Math.round(ms / 60000);
    const h = Math.floor(totalMin / 60) % 24;
    const m = totalMin % 60;
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
  }

  private normalizeTime(time: string) {
    return time.substring(0, 5);
  }

  private today() {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  }
}
