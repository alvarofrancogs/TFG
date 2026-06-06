package com.alvar.oasisclub.payments.repository;

import com.alvar.oasisclub.payments.entity.StripeProcessedEventEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeProcessedEventRepository extends JpaRepository<StripeProcessedEventEntity, UUID> {

  boolean existsByStripeEventId(String stripeEventId);

  Optional<StripeProcessedEventEntity> findTopByOrderByProcessedAtDesc();
}
