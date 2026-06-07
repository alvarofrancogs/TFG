import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthStore } from '../../auth.store';
import { ProfileApiService } from '../../servicios/profile-api.service';
import { ProfileReservation, ProfileSummary } from '../../modelos/profile.models';
import { ReservationsApiService } from '../../servicios/reservations-api.service';
import { GymApiService } from '../../servicios/gym-api.service';
import { RoutineDay } from '../../modelos/gym.models';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './perfil.component.html',
  styleUrl: './perfil.component.css',
})
export class PerfilComponent implements OnInit {
  auth = inject(AuthStore);
  private authStore = inject(AuthStore);
  private profileApi = inject(ProfileApiService);
  private reservationsApi = inject(ReservationsApiService);
  private gymApi = inject(GymApiService);

  profile = signal<ProfileSummary | null>(null);
  myReservations = signal<ProfileReservation[]>([]);
  myRoutine = signal<RoutineDay[]>([]);
  loading = signal(true);

  
  confirmDeleteReservationId = signal<string | null>(null);
  confirmDeleteDayId = signal<number | null>(null);
  confirmDeleteExerciseId = signal<string | null>(null);

  ngOnInit() {
    const session = this.authStore.session();
    if (!session) return;

    this.profileApi.getByClient(session.clientId).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.myReservations.set(profile.reservations);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });

    this.gymApi.getByClient(session.clientId).subscribe({
      next: (routine) => this.myRoutine.set(routine),
    });
  }

  deleteReservation(reservationId: string) {
    this.reservationsApi.delete(reservationId).subscribe({
      next: () => {
        this.confirmDeleteReservationId.set(null);
        this.myReservations.update((res) => res.filter((r) => r.id !== reservationId));
      },
    });
  }

  requestDeleteReservation(id: string) {
    this.confirmDeleteReservationId.set(id);
  }

  cancelDeleteReservation() {
    this.confirmDeleteReservationId.set(null);
  }

  requestDeleteDay(dayId: number) {
    this.confirmDeleteDayId.set(dayId);
  }

  cancelDeleteDay() {
    this.confirmDeleteDayId.set(null);
  }

  requestDeleteExercise(exerciseId: string) {
    this.confirmDeleteExerciseId.set(exerciseId);
  }

  cancelDeleteExercise() {
    this.confirmDeleteExerciseId.set(null);
  }

  deleteExercise(dayId: number, exerciseId?: string) {
    const session = this.authStore.session();
    if (!session || !exerciseId) return;

    const updatedRoutine = this.myRoutine().map((day) => {
      if (day.dayOrder !== dayId) return day;
      return {
        ...day,
        exercises: day.exercises.filter((e) => e.id !== exerciseId),
      };
    });

    this.myRoutine.set(updatedRoutine);
    this.confirmDeleteExerciseId.set(null);
    this.gymApi.update(session.clientId, { days: updatedRoutine }).subscribe();
  }

  deleteDay(dayId: number) {
    const session = this.authStore.session();
    if (!session) return;

    const updatedRoutine = this.myRoutine().filter((day) => day.dayOrder !== dayId);
    this.myRoutine.set(updatedRoutine);
    this.confirmDeleteDayId.set(null);
    this.gymApi.update(session.clientId, { days: updatedRoutine }).subscribe();
  }

  statusLabel(status: string) {
    if (status === 'PENDING') return 'Pendiente de pago';
    if (status === 'CONFIRMED') return 'Confirmada';
    if (status === 'MAINTENANCE') return 'Mantenimiento';
    return status;
  }

  monthAbbr(dateStr: string): string {
    const months = ['Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic'];
    const parts = dateStr.split('-');
    const m = parseInt(parts[1] ?? '1', 10) - 1;
    return months[m] ?? '';
  }
}
