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
}