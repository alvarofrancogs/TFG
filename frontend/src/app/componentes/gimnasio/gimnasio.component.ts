import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';

import {AuthStore} from '../../auth.store';
import {RoutineDay, RoutineExercise} from '../../modelos/gym.models';
import {GymApiService} from '../../servicios/gym-api.service';

type EditableExerciseField = 'name' | 'sets' | 'reps' | 'rest';

@Component({
  selector: 'app-gimnasio',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './gimnasio.component.html',
  styleUrl: './gimnasio.component.css',
})
export class GimnasioComponent implements OnInit {
}