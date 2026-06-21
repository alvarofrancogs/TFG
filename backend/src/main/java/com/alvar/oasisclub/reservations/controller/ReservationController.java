package com.alvar.oasisclub.reservations.controller;

import com.alvar.oasisclub.auth.security.AccessControlService;
import com.alvar.oasisclub.auth.security.AuthenticatedUser;
import com.alvar.oasisclub.clients.entity.ClientEntity;
import com.alvar.oasisclub.clients.service.ClientService;
import com.alvar.oasisclub.reservations.dto.AvailabilitySlotResponse;
import com.alvar.oasisclub.reservations.dto.CreateMaintenanceBlockRequest;
import com.alvar.oasisclub.reservations.dto.CreateReservationRequest;
import com.alvar.oasisclub.reservations.dto.ReservationResponse;
import com.alvar.oasisclub.reservations.entity.ReservationEntity;
import com.alvar.oasisclub.reservations.entity.SportType;
import com.alvar.oasisclub.reservations.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
@Tag(name = "Reservas", description = "Gestión de reservas de pistas, disponibilidad y bloques de mantenimiento")
public class ReservationController {

  private final ReservationService reservationService;
  private final AccessControlService accessControl;
  private final ClientService clientService;

  @GetMapping("/reservations")
  @Operation(
      summary = "Listar reservas",
      description = "Devuelve la lista de reservas. Los administradores ven todas las reservas; los clientes solo ven las suyas. Se puede filtrar por deporte, estado y fecha."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lista de reservas obtenida correctamente"),
      @ApiResponse(responseCode = "401", description = "No autenticado")
  })
  public List<ReservationResponse> getReservations(
      @Parameter(description = "Filtrar por tipo de deporte (PADEL, TENNIS, etc.)") @RequestParam(required = false) String sport,
      @Parameter(description = "Filtrar por estado (CONFIRMED, PENDING, CANCELLED)") @RequestParam(required = false) String status,
      @Parameter(description = "Filtrar por fecha en formato YYYY-MM-DD") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      Authentication authentication
  ) {
    if (!accessControl.isAdmin(authentication)) {
      AuthenticatedUser user = accessControl.requireUser(authentication);
      return reservationService.getReservationsByClient(user.clientId(), sport, status, date);
    }

    return reservationService.getReservations(sport, status, date);
  }

  @PostMapping("/reservations")
  @Operation(
      summary = "Crear reserva (solo administrador)",
      description = "Crea una reserva confirmada directamente para un cliente. Solo disponible para administradores. Los clientes deben pasar por el flujo de pago de Stripe."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Reserva creada correctamente"),
      @ApiResponse(responseCode = "400", description = "Datos inválidos o cliente no especificado"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "403", description = "Solo los administradores pueden crear reservas directamente")
  })
  public ResponseEntity<ReservationResponse> createReservation(
      @Valid @RequestBody CreateReservationRequest request,
      Authentication authentication
  ) {
    accessControl.requireUser(authentication);
    boolean isAdmin = accessControl.isAdmin(authentication);

    if (!isAdmin) {
      throw new org.springframework.security.access.AccessDeniedException(
          "Los socios deben pagar las reservas mediante el proceso de pago"
      );
    }

    UUID effectiveClientId;
    if (request.getClientId() == null || request.getClientId().isBlank()) {
      throw new IllegalArgumentException("El cliente es obligatorio para crear una reserva como administrador");
    }
    effectiveClientId = UUID.fromString(request.getClientId());

    ClientEntity targetClient = clientService.getEntityById(effectiveClientId);

    request.setClientId(targetClient.getId().toString());
    request.setUserName(targetClient.getName());

    return ResponseEntity.ok(reservationService.createConfirmedReservation(request));
  }

  @PostMapping("/reservations/maintenance")
  @Operation(
      summary = "Crear bloque de mantenimiento",
      description = "Bloquea una pista en una franja horaria específica por motivos de mantenimiento. Solo accesible por administradores."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Bloque de mantenimiento creado correctamente"),
      @ApiResponse(responseCode = "400", description = "Datos del bloque inválidos"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "403", description = "Solo los administradores pueden crear bloques de mantenimiento")
  })
  public ResponseEntity<ReservationResponse> createMaintenance(
      @Valid @RequestBody CreateMaintenanceBlockRequest request,
      Authentication authentication
  ) {
    accessControl.requireAdmin(authentication);
    return ResponseEntity.ok(reservationService.createMaintenanceBlock(request));
  }

  @DeleteMapping("/reservations/{id}")
  @Operation(
      summary = "Cancelar reserva",
      description = "Elimina una reserva existente. Los administradores pueden eliminar cualquier reserva; los clientes solo pueden cancelar las suyas propias."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Reserva cancelada correctamente"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "403", description = "Sin permisos para cancelar esta reserva"),
      @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
  })
  public ResponseEntity<Void> deleteReservation(
      @Parameter(description = "ID UUID de la reserva") @PathVariable UUID id,
      Authentication authentication
  ) {
    if (!accessControl.isAdmin(authentication)) {
      AuthenticatedUser user = accessControl.requireUser(authentication);
      ReservationEntity reservation = reservationService.getEntityById(id);
      if (reservation.getClientId() == null || !reservation.getClientId().equals(user.clientId())) {
        throw new org.springframework.security.access.AccessDeniedException("No tienes permiso para eliminar esta reserva");
      }
    }

    reservationService.deleteReservation(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/availability")
  @Operation(
      summary = "Consultar disponibilidad de una pista",
      description = "Devuelve las franjas horarias disponibles y ocupadas de una pista específica en una fecha concreta."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Disponibilidad obtenida correctamente"),
      @ApiResponse(responseCode = "400", description = "Parámetros inválidos"),
      @ApiResponse(responseCode = "404", description = "Pista no encontrada")
  })
  public List<AvailabilitySlotResponse> getAvailability(
      @Parameter(description = "ID UUID de la pista") @RequestParam UUID courtId,
      @Parameter(description = "Fecha en formato YYYY-MM-DD") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
  ) {
    return reservationService.getAvailability(courtId, date);
  }
}
