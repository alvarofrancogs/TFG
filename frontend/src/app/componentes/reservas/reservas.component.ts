import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthStore } from '../../auth.store';
import { CourtResponse } from '../../modelos/court.model';
import { AvailabilitySlot } from '../../modelos/reservation.models';
import { CourtApiService } from '../../servicios/court-api.service';
import { PaymentsApiService } from '../../servicios/payments-api.service';
import { ReservationsApiService } from '../../servicios/reservations-api.service';

@Component({
  selector: 'app-reservas',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reservas.component.html',
  styleUrls: ['./reservas.component.css'],
})
export class ReservasComponent implements OnInit {
}