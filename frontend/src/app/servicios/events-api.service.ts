import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { CreateEventRequest, EventRegistration, EventResponse } from '../modelos/event.models';

@Injectable({ providedIn: 'root' })
export class EventsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  getActiveEvents(): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(`${this.baseUrl}/events`);
  }

  getAllEvents(): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(`${this.baseUrl}/events/all`);
  }

  createEvent(request: CreateEventRequest): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${this.baseUrl}/events`, request);
  }

  deleteEvent(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/events/${id}`);
  }

  getRegistrations(eventId: string): Observable<EventRegistration[]> {
    return this.http.get<EventRegistration[]>(`${this.baseUrl}/events/${eventId}/registrations`);
  }

  removeRegistration(eventId: string, regId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/events/${eventId}/registrations/${regId}`);
  }

  register(eventId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/events/${eventId}/register`, {});
  }

  unregister(eventId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/events/${eventId}/register`);
  }
}
