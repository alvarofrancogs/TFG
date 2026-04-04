package com.alvar.oasisclub.events.dto;

import com.alvar.oasisclub.events.entity.EventCategory;
import com.alvar.oasisclub.reservations.entity.SportType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateEventRequest {

  @NotBlank
  private String title;

  private String description;

  @NotNull
  private LocalDate eventDate;

  @NotNull
  private LocalTime startTime;

  @NotNull
  private LocalTime endTime;

  @NotNull
  @Min(1)
  @Max(10_000)
  private Integer maxCapacity;

  @NotNull
  private EventCategory category;

  private SportType sport;

  private List<UUID> courtIds;
}
