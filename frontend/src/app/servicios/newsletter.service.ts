import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class NewsletterService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  subscribe(email: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/newsletter/subscribe`, { email });
  }

  unsubscribe(email: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/newsletter/unsubscribe`, { email });
  }

  checkStatus(): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/newsletter/status`);
  }
}
