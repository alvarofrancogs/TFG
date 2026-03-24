package com.alvar.oasisclub.courts;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alvar.oasisclub.courts.entity.CourtEntity;
import com.alvar.oasisclub.courts.exception.CourtNotFoundException;
import com.alvar.oasisclub.courts.mapper.CourtMapper;
import com.alvar.oasisclub.courts.repository.CourtRepository;
import com.alvar.oasisclub.courts.service.CourtService;
import com.alvar.oasisclub.reservations.entity.ReservationEntity;
import com.alvar.oasisclub.reservations.entity.ReservationStatus;
import com.alvar.oasisclub.reservations.entity.SportType;
import com.alvar.oasisclub.reservations.repository.ReservationRepository;
import com.alvar.oasisclub.reservations.service.ReservationService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtServiceTest {

  @Mock
  private CourtRepository courtRepository;

  @Mock
  private CourtMapper courtMapper;

  @Mock
  private ReservationRepository reservationRepository;

  @Mock
  private ReservationService reservationService;

  @InjectMocks
  private CourtService courtService;

  private CourtEntity court(UUID id) {
    CourtEntity court = new CourtEntity();
    court.setId(id);
    court.setName("Pista 1");
    court.setSport(SportType.PADEL);
    court.setIsActive(true);
    return court;
  }

  private ReservationEntity reservation(UUID id, ReservationStatus status) {
    return ReservationEntity.builder()
        .id(id)
        .status(status)
        .build();
  }

  @Test
  void deleteCourtCancelsActiveReservationsAndDeletesCourt() {
    UUID courtId = UUID.randomUUID();
    CourtEntity court = court(courtId);

    ReservationEntity confirmed = reservation(UUID.randomUUID(), ReservationStatus.CONFIRMED);
    ReservationEntity pending = reservation(UUID.randomUUID(), ReservationStatus.PENDING);

    when(courtRepository.findById(courtId)).thenReturn(Optional.of(court));
    when(reservationRepository.findByCourt_Id(courtId)).thenReturn(List.of(confirmed, pending));

    courtService.deleteCourt(courtId);

    verify(reservationService).cancelAndRefundIsolated(confirmed);
    verify(reservationService).cancelAndRefundIsolated(pending);
    verify(courtRepository).delete(court);
  }

  @Test
  void deleteCourtRemovesMaintenanceBlocksDirectlyWithoutRefund() {
    UUID courtId = UUID.randomUUID();
    CourtEntity court = court(courtId);

    ReservationEntity maintenance = reservation(UUID.randomUUID(), ReservationStatus.MAINTENANCE);

    when(courtRepository.findById(courtId)).thenReturn(Optional.of(court));
    when(reservationRepository.findByCourt_Id(courtId)).thenReturn(List.of(maintenance));

    courtService.deleteCourt(courtId);

    verify(reservationRepository).delete(maintenance);
    verify(reservationService, never()).cancelAndRefundIsolated(any());
    verify(courtRepository).delete(court);
  }

  @Test
  void deleteCourtAbortsWhenAReservationCannotBeRefunded() {
    UUID courtId = UUID.randomUUID();
    CourtEntity court = court(courtId);

    ReservationEntity confirmed = reservation(UUID.randomUUID(), ReservationStatus.CONFIRMED);

    when(courtRepository.findById(courtId)).thenReturn(Optional.of(court));
    when(reservationRepository.findByCourt_Id(courtId)).thenReturn(List.of(confirmed));
    doThrow(new RuntimeException("Stripe down"))
        .when(reservationService).cancelAndRefundIsolated(confirmed);

    assertThrows(IllegalStateException.class, () -> courtService.deleteCourt(courtId));

    
    verify(courtRepository, never()).delete(court);
  }

  @Test
  void deleteCourtWithNoReservationsJustDeletesCourt() {
    UUID courtId = UUID.randomUUID();
    CourtEntity court = court(courtId);

    when(courtRepository.findById(courtId)).thenReturn(Optional.of(court));
    when(reservationRepository.findByCourt_Id(courtId)).thenReturn(List.of());

    courtService.deleteCourt(courtId);

    verify(reservationService, never()).cancelAndRefundIsolated(any());
    verify(courtRepository).delete(court);
  }

  @Test
  void deleteMissingCourtThrows() {
    UUID courtId = UUID.randomUUID();
    when(courtRepository.findById(courtId)).thenReturn(Optional.empty());

    assertThrows(CourtNotFoundException.class, () -> courtService.deleteCourt(courtId));
    verify(courtRepository, never()).delete(any());
  }
}
