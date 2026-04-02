package com.alvar.oasisclub.events.repository;

import com.alvar.oasisclub.events.entity.EventRegistrationEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRegistrationRepository extends JpaRepository<EventRegistrationEntity, UUID> {

  List<EventRegistrationEntity> findByEventIdOrderByCreatedAtAsc(UUID eventId);

  Optional<EventRegistrationEntity> findByEventIdAndClientId(UUID eventId, UUID clientId);

  long countByEventId(UUID eventId);

  void deleteByEventIdAndClientId(UUID eventId, UUID clientId);

  boolean existsByEventIdAndClientId(UUID eventId, UUID clientId);
}
