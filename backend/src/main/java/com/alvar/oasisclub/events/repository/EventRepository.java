package com.alvar.oasisclub.events.repository;

import com.alvar.oasisclub.events.entity.EventCategory;
import com.alvar.oasisclub.events.entity.EventEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {

  List<EventEntity> findAllByOrderByEventDateAscStartTimeAsc();

  List<EventEntity> findByIsActiveTrueAndEventDateGreaterThanEqualOrderByEventDateAscStartTimeAsc(LocalDate date);

  List<EventEntity> findByEventDateBefore(LocalDate date);
}
