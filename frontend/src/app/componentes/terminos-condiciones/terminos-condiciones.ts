import { Component } from '@angular/core';

@Component({
  selector: 'app-terminos-condiciones',
  imports: [],
  templateUrl: './terminos-condiciones.html',
  styleUrl: './terminos-condiciones.css',
})
export class TerminosCondiciones {
  scrollTo(id: string): void {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }
}
