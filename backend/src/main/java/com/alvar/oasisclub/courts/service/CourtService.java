package com.alvar.oasisclub.courts.service;

import com.alvar.oasisclub.courts.dto.CourtResponse;
import com.alvar.oasisclub.courts.dto.CreateCourtRequest;
import com.alvar.oasisclub.courts.entity.CourtEntity;
import com.alvar.oasisclub.courts.exception.CourtNotFoundException;
import com.alvar.oasisclub.courts.mapper.CourtMapper;
import com.alvar.oasisclub.courts.repository.CourtRepository;
import com.alvar.oasisclub.reservations.entity.ReservationEntity;
import com.alvar.oasisclub.reservations.entity.ReservationStatus;
import com.alvar.oasisclub.reservations.entity.SportType;
import com.alvar.oasisclub.reservations.repository.ReservationRepository;
import com.alvar.oasisclub.reservations.service.ReservationService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourtService {

    private static final Logger log = LoggerFactory.getLogger(CourtService.class);

    private final CourtRepository courtRepository;
    private final CourtMapper courtMapper;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    public CourtService(
        CourtRepository courtRepository,
        CourtMapper courtMapper,
        ReservationRepository reservationRepository,
        @Lazy ReservationService reservationService
    ) {
        this.courtRepository = courtRepository;
        this.courtMapper = courtMapper;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
    }

    @Transactional(readOnly = true)
    public List<CourtResponse> getAllCourts(SportType sport) {
        if (sport != null) {
            return courtRepository.findBySportOrderByCreatedAtAsc(sport)
                .stream().map(courtMapper::toResponse).toList();
        }
        return courtRepository.findAll().stream().map(courtMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CourtResponse> getActiveCourts(SportType sport) {
        if (sport != null) {
            return courtRepository.findBySportAndIsActiveOrderByCreatedAtAsc(sport, true)
                .stream().map(courtMapper::toResponse).toList();
        }
        return courtRepository.findByIsActiveOrderByCreatedAtAsc(true)
            .stream().map(courtMapper::toResponse).toList();
    }

    @Transactional
    public CourtResponse createCourt(CreateCourtRequest request) {
        CourtEntity saved = courtRepository.save(courtMapper.fromCreateRequest(request));
        return courtMapper.toResponse(saved);
    }

    
    @Transactional
    public void deleteCourt(UUID id) {
        CourtEntity court = courtRepository.findById(id)
            .orElseThrow(() -> new CourtNotFoundException("Pista no encontrada"));

        List<ReservationEntity> reservations = reservationRepository.findByCourt_Id(id);

        int refundFailures = 0;
        for (ReservationEntity reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.MAINTENANCE) {
                reservationRepository.delete(reservation);
                continue;
            }
            try {
                reservationService.cancelAndRefundIsolated(reservation);
            } catch (Exception ex) {
                refundFailures++;
                log.error("Could not cancel reservation {} while deleting court {}: {}",
                    reservation.getId(), id, ex.getMessage());
            }
        }

        if (refundFailures > 0) {
            
            throw new IllegalStateException(
                "No se pudo eliminar la pista: " + refundFailures
                + " reserva(s) no se pudieron cancelar/reembolsar. Inténtalo de nuevo más tarde.");
        }

        courtRepository.delete(court);
        log.info("Court {} deleted after cancelling {} reservation(s)", id, reservations.size());
    }
}

