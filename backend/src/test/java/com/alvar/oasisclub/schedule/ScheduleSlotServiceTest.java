package com.alvar.oasisclub.schedule;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alvar.oasisclub.reservations.entity.ReservationEntity;
import com.alvar.oasisclub.reservations.entity.ReservationStatus;
import com.alvar.oasisclub.reservations.repository.ReservationRepository;
import com.alvar.oasisclub.reservations.service.ReservationService;
import com.alvar.oasisclub.schedule.repository.ScheduleSlotRepository;
import com.alvar.oasisclub.schedule.service.ScheduleSlotService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleSlotServiceTest {

  @Mock
  private ScheduleSlotRepository scheduleSlotRepository;

  @Mock
  private ReservationRepository reservationRepository;

  @Mock
  private ReservationService reservationService;

  @InjectMocks
  private ScheduleSlotService scheduleSlotService;

  private ReservationEntity reservation(UUID id, ReservationStatus status) {
    return ReservationEntity.builder()
        .id(id)
        .status(status)
        .build();
  }

  @Test
  void removeSlotCancelsReservationsAndDeletesSlot() {
    LocalTime parsed = LocalTime.of(18, 0);
    ReservationEntity confirmed = reservation(UUID.randomUUID(), ReservationStatus.CONFIRMED);

    when(scheduleSlotRepository.existsBySlotTime(parsed)).thenReturn(true);
    when(reservationRepository.findByReservationTimeAndReservationDateGreaterThanEqualAndStatusIn(
        eq(parsed), eq(LocalDate.now()), anyList()))
        .thenReturn(List.of(confirmed));

    scheduleSlotService.removeSlot("18:00");

    verify(reservationService).cancelAndRefundIsolated(confirmed);
    verify(scheduleSlotRepository).deleteBySlotTime(parsed);
  }

  @Test
  void removeSlotDeletesMaintenanceBlocksDirectly() {
    LocalTime parsed = LocalTime.of(18, 0);
    ReservationEntity maintenance = reservation(UUID.randomUUID(), ReservationStatus.MAINTENANCE);

    when(scheduleSlotRepository.existsBySlotTime(parsed)).thenReturn(true);
    when(reservationRepository.findByReservationTimeAndReservationDateGreaterThanEqualAndStatusIn(
        eq(parsed), eq(LocalDate.now()), anyList()))
        .thenReturn(List.of(maintenance));

    scheduleSlotService.removeSlot("18:00");

    verify(reservationRepository).delete(maintenance);
    verify(reservationService, never()).cancelAndRefundIsolated(any());
    verify(scheduleSlotRepository).deleteBySlotTime(parsed);
  }

  @Test
  void removeSlotAbortsWhenACancellationFails() {
    LocalTime parsed = LocalTime.of(18, 0);
    ReservationEntity confirmed = reservation(UUID.randomUUID(), ReservationStatus.CONFIRMED);

    when(scheduleSlotRepository.existsBySlotTime(parsed)).thenReturn(true);
    when(reservationRepository.findByReservationTimeAndReservationDateGreaterThanEqualAndStatusIn(
        eq(parsed), eq(LocalDate.now()), anyList()))
        .thenReturn(List.of(confirmed));
    doThrow(new RuntimeException("Stripe down"))
        .when(reservationService).cancelAndRefundIsolated(confirmed);

    assertThrows(IllegalStateException.class, () -> scheduleSlotService.removeSlot("18:00"));

    
    verify(scheduleSlotRepository, never()).deleteBySlotTime(parsed);
  }

  @Test
  void removeMissingSlotThrows() {
    LocalTime parsed = LocalTime.of(18, 0);
    when(scheduleSlotRepository.existsBySlotTime(parsed)).thenReturn(false);

    assertThrows(IllegalArgumentException.class, () -> scheduleSlotService.removeSlot("18:00"));
    verify(scheduleSlotRepository, never()).deleteBySlotTime(any());
  }
}
