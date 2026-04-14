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
}