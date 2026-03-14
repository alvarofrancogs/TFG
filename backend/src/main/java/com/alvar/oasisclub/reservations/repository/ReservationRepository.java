package com.alvar.oasisclub.reservations.repository;

import com.alvar.oasisclub.reservations.entity.ReservationEntity;
import com.alvar.oasisclub.reservations.entity.ReservationStatus;
import com.alvar.oasisclub.reservations.entity.SportType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<ReservationEntity, UUID> {

  List<ReservationEntity> findAllByOrderByReservationDateDescReservationTimeDesc();

  List<ReservationEntity> findBySportOrderByReservationDateDescReservationTimeDesc(SportType sport);

  List<ReservationEntity> findByStatusOrderByReservationDateDescReservationTimeDesc(ReservationStatus status);

  List<ReservationEntity> findByReservationDateOrderByReservationTimeAsc(LocalDate date);

  List<ReservationEntity> findBySportAndStatusAndReservationDateOrderByReservationTimeAsc(
      SportType sport,
      ReservationStatus status,
      LocalDate date
  );

  List<ReservationEntity> findByCourt_IdAndReservationDateOrderByReservationTimeAsc(UUID courtId, LocalDate date);

  List<ReservationEntity> findByCourt_Id(UUID courtId);

  List<ReservationEntity> findByClientIdOrderByReservationDateDescReservationTimeDesc(UUID clientId);

  List<ReservationEntity> findByClientIdAndReservationDateOrderByReservationTimeAsc(UUID clientId, LocalDate date);

  Optional<ReservationEntity> findByStripeSessionId(String stripeSessionId);

  Optional<ReservationEntity> findByStripeSessionIdAndClientId(String stripeSessionId, UUID clientId);

  boolean existsByCourt_IdAndReservationDateAndReservationTime(UUID courtId, LocalDate date, LocalTime time);

  Optional<ReservationEntity> findByCourt_IdAndReservationDateAndReservationTime(UUID courtId, LocalDate date, LocalTime time);

  List<ReservationEntity> findByReservationTimeAndReservationDateGreaterThanEqualAndStatusIn(
      LocalTime time, LocalDate date, List<ReservationStatus> statuses
  );

  List<ReservationEntity> findByStatusAndReservationDateAndReservationTimeBetween(
      ReservationStatus status,
      LocalDate date,
      LocalTime startTime,
      LocalTime endTime
  );

  
  @org.springframework.data.jpa.repository.Query("""
      SELECT r FROM ReservationEntity r
      WHERE r.status = com.alvar.oasisclub.reservations.entity.ReservationStatus.PENDING
        AND r.stripeSessionId IS NOT NULL
        AND r.stripeSessionId <> ''
        AND r.createdAt <= :cutoff
      """)
  List<ReservationEntity> findStalePendingWithStripeSession(
      @org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff
  );

  
  @org.springframework.data.jpa.repository.Query("""
      SELECT r FROM ReservationEntity r
      WHERE r.status = com.alvar.oasisclub.reservations.entity.ReservationStatus.CONFIRMED
        AND r.stripeSessionId IS NOT NULL
        AND r.stripeSessionId <> ''
        AND r.refundedAt IS NULL
      """)
  List<ReservationEntity> findConfirmedWithStripeSessionAndNoRefund();

  
  @org.springframework.data.jpa.repository.Modifying
  @org.springframework.data.jpa.repository.Query("""
      DELETE FROM ReservationEntity r
      WHERE r.status = com.alvar.oasisclub.reservations.entity.ReservationStatus.MAINTENANCE
        AND r.reservationDate = :date
        AND r.reservationTime >= :startTime
        AND r.reservationTime < :endTime
      """)
  int deleteMaintenanceBlocksByDateAndTimeRange(
      @org.springframework.data.repository.query.Param("date") LocalDate date,
      @org.springframework.data.repository.query.Param("startTime") LocalTime startTime,
      @org.springframework.data.repository.query.Param("endTime") LocalTime endTime
  );
}

