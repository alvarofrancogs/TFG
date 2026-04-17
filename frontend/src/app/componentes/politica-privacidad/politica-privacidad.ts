import { Component } from '@angular/core';

@Component({
  selector: 'app-politica-privacidad',
  imports: [],
  templateUrl: './politica-privacidad.html',
  styleUrl: './politica-privacidad.css',
})
export class PoliticaPrivacidad {
  scrollTo(id: string): void {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }
}
