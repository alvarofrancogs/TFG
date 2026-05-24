import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CourtResponse } from '../../modelos/court.model';
import { CreateMaintenanceBlockRequest, Reservation, SportType } from '../../modelos/reservation.models';
import { CourtApiService } from '../../servicios/court-api.service';
import { ReservationsApiService } from '../../servicios/reservations-api.service';
import { ScheduleApiService } from '../../servicios/schedule-api.service';

@Component({
  selector: 'app-admin-reservas',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './admin-reservas.component.html',
  styleUrl: './admin-reservas.component.css',
})
export class AdminReservasComponent implements OnInit {
}