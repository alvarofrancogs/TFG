export type EventCategory = 'TORNEO' | 'LIGA' | 'MASTERCLASS' | 'SOCIAL';

export interface EventResponse {
  id: string;
  title: string;
  description: string;
  eventDate: string;
  startTime: string;
  endTime: string;
  maxCapacity: number;
  registeredCount: number;
  isRegistered: boolean;
  category: EventCategory;
  sport: string | null;
  courtNames: string | null;
  isActive: boolean;
}

export interface CreateEventRequest {
  title: string;
  description: string;
  eventDate: string;
  startTime: string;
  endTime: string;
  maxCapacity: number;
  category: EventCategory;
  sport?: string;
  courtIds?: string[];
}

export interface EventRegistration {
  id: string;
  clientId: string;
  clientName: string;
  clientEmail: string;
  registeredAt: string;
}
