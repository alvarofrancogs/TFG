package com.alvar.oasisclub.gym;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alvar.oasisclub.gym.dto.RoutineDayDto;
import com.alvar.oasisclub.gym.dto.RoutineExerciseDto;
import com.alvar.oasisclub.gym.entity.GymRoutineDayEntity;
import com.alvar.oasisclub.gym.entity.GymRoutineExerciseEntity;
import com.alvar.oasisclub.gym.mapper.GymMapper;
import com.alvar.oasisclub.gym.repository.GymRoutineDayRepository;
import com.alvar.oasisclub.gym.repository.GymRoutineExerciseRepository;
import com.alvar.oasisclub.gym.service.GymService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GymServiceTest {
}