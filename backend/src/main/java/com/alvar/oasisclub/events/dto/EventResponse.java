package com.alvar.oasisclub.events.dto;

import com.alvar.oasisclub.events.entity.EventCategory;
import com.alvar.oasisclub.reservations.entity.SportType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record EventResponse(
    UUID id,
    String title,
    String description,
    LocalDate eventDate,
    LocalTime startTime,
    LocalTime endTime,
    int maxCapacity,
    long registeredCount,
    boolean isRegistered,
    EventCategory category,
    SportType sport,
    String courtNames,
    boolean isActive,
    LocalDateTime createdAt
) {}
