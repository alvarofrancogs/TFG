import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthStore } from '../../auth.store';

interface Tarifa {
  badge: string;
  image: string;
  sportLabel: string;
  title: string;
  features: string[];
  amount: string;
  unit: string;
  ctaText: string;
  ctaRoute: string;
}

@Component({
  selector: 'app-tarifas',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './tarifas.component.html',
  styleUrl: './tarifas.component.css',
})
export class TarifasComponent {
  readonly authStore = inject(AuthStore);

  tarifas: Tarifa[] = [
    {
      badge: 'Pistas',
      image: 'images/sport-padel-ultrareal.png',
      sportLabel: 'Deporte de raqueta',
      title: 'Reserva de Pista de Pádel',
      features: [
        '4 pistas indoor profesionales de cristal panorámico',
        'Iluminación LED de última generación sin sombras',
        'Acceso a vestuario premium y lounge social',
      ],
      amount: '50€',
      unit: 'por pista / 90 min',
      ctaText: 'Reservar pista',
      ctaRoute: '/reservar',
    },
    {
      badge: 'Campo',
      image: 'images/sport-futbol-ultrareal.png',
      sportLabel: 'Deporte de equipo',
      title: 'Reserva de Campo de Fútbol Indoor',
      features: [
        'Césped artificial de calidad profesional homologado',
        'Iluminación LED de alta potencia para partidos nocturnos',
        'Gradas laterales para público y vestuarios premium',
      ],
      amount: '100€',
      unit: 'por campo / 90 min',
      ctaText: 'Reservar campo',
      ctaRoute: '/reservar',
    },
    {
      badge: 'Socio',
      image: 'images/sport-gimnasio-ultrareal.png',
      sportLabel: 'Entrenamiento',
      title: 'Suscripción Mensual Gimnasio',
      features: [
        'Acceso ilimitado a zona de fuerza y máquinas guiadas',
        'Asesoramiento y rutinas personalizadas en la app',
        'Servicio de toallas y taquilla diaria incluidos',
      ],
      amount: '35€',
      unit: 'al mes / acceso total',
      ctaText: 'Hacerse socio',
      ctaRoute: '/registro',
    },
  ];
}

