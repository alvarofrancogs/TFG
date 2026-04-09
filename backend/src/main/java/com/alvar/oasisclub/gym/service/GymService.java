package com.alvar.oasisclub.gym.service;

import com.alvar.oasisclub.gym.dto.RoutineDayDto;
import com.alvar.oasisclub.gym.dto.RoutineExerciseDto;
import com.alvar.oasisclub.gym.dto.UpdateRoutineRequest;
import com.alvar.oasisclub.gym.entity.GymRoutineDayEntity;
import com.alvar.oasisclub.gym.entity.GymRoutineExerciseEntity;
import com.alvar.oasisclub.gym.mapper.GymMapper;
import com.alvar.oasisclub.gym.repository.GymRoutineDayRepository;
import com.alvar.oasisclub.gym.repository.GymRoutineExerciseRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class GymService {

  private final GymRoutineDayRepository dayRepository;
  private final GymRoutineExerciseRepository exerciseRepository;
  private final GymMapper gymMapper;

  @Transactional(readOnly = true)
  public List<RoutineDayDto> getRoutine(UUID clientId) {
    List<GymRoutineDayEntity> days = dayRepository.findByClientIdOrderByDayOrderAsc(clientId);
    List<RoutineDayDto> response = new ArrayList<>();

    for (GymRoutineDayEntity day : days) {
      RoutineDayDto dayDto = gymMapper.toDayDto(day);
      List<GymRoutineExerciseEntity> exercises = exerciseRepository.findByRoutineDayIdOrderByExerciseOrderAsc(day.getId());
      List<RoutineExerciseDto> exerciseDtos = exercises.stream().map(gymMapper::toExerciseDto).toList();
      dayDto.setExercises(exerciseDtos);
      response.add(dayDto);
    }

    return response;
  }

  @Transactional
  public List<RoutineDayDto> updateRoutine(UUID clientId, UpdateRoutineRequest request) {
    List<GymRoutineDayEntity> currentDays = dayRepository.findByClientIdOrderByDayOrderAsc(clientId);
    if (!currentDays.isEmpty()) {
      List<UUID> dayIds = currentDays.stream().map(GymRoutineDayEntity::getId).toList();
      exerciseRepository.deleteByRoutineDayIdIn(dayIds);
      dayRepository.deleteByClientId(clientId);
    }

    int dayOrder = 1;
    for (RoutineDayDto dayDto : request.getDays()) {
      GymRoutineDayEntity dayEntity = GymRoutineDayEntity.builder()
          .clientId(clientId)
          .dayOrder(dayDto.getDayOrder() != null ? dayDto.getDayOrder() : dayOrder)
          .name(dayDto.getName() == null ? "" : dayDto.getName())
          .build();
      GymRoutineDayEntity savedDay = dayRepository.save(dayEntity);

      int exerciseOrder = 1;
      for (RoutineExerciseDto exerciseDto : dayDto.getExercises()) {
        GymRoutineExerciseEntity exercise = GymRoutineExerciseEntity.builder()
            .routineDayId(savedDay.getId())
            .exerciseOrder(exerciseDto.getOrder() != null ? exerciseDto.getOrder() : exerciseOrder)
            .name(exerciseDto.getName() == null ? "" : exerciseDto.getName())
            .setsCount(exerciseDto.getSets() == null ? 3 : exerciseDto.getSets())
            .reps(exerciseDto.getReps() == null ? "10" : exerciseDto.getReps())
            .restInterval(exerciseDto.getRest() == null ? "60s" : exerciseDto.getRest())
            .build();
        exerciseRepository.save(exercise);
        exerciseOrder++;
      }
      dayOrder++;
    }

    return getRoutine(clientId);
  }

  
  @Transactional
  public void seedDefaultRoutine(UUID clientId) {
    GymRoutineDayEntity day = dayRepository.save(GymRoutineDayEntity.builder()
        .clientId(clientId)
        .dayOrder(1)
        .name("Fuerza")
        .build());

    String[][] defaults = {
        {"Sentadilla Libre",  "4", "10",   "90s"},
        {"Press de Banca",    "4", "8",    "90s"},
        {"Dominadas",         "3", "10",   "60s"},
        {"Plancha Abdominal", "3", "60s",  "45s"},
    };

    int order = 1;
    for (String[] e : defaults) {
      exerciseRepository.save(GymRoutineExerciseEntity.builder()
          .routineDayId(day.getId())
          .exerciseOrder(order++)
          .name(e[0])
          .setsCount(Integer.parseInt(e[1]))
          .reps(e[2])
          .restInterval(e[3])
          .build());
    }
  }
}

