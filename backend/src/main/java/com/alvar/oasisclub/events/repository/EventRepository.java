package com.alvar.oasisclub.events.repository;

import com.alvar.oasisclub.events.entity.EventCategory;
import com.alvar.oasisclub.events.entity.EventEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT e FROM EventEntity e WHERE e.id = :id")
  Optional<EventEntity> findByIdForUpdate(@Param("id") UUID id);

  List<EventEntity> findAllByOrderByEventDateAscStartTimeAsc();

  List<EventEntity> findByIsActiveTrueAndEventDateGreaterThanEqualOrderByEventDateAscStartTimeAsc(LocalDate date);

  List<EventEntity> findByEventDateBefore(LocalDate date);
}
