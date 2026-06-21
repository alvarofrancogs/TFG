package com.alvar.oasisclub.events.controller;

import com.alvar.oasisclub.auth.security.AccessControlService;
import com.alvar.oasisclub.auth.security.AuthenticatedUser;
import com.alvar.oasisclub.events.dto.CreateEventRequest;
import com.alvar.oasisclub.events.dto.EventRegistrationResponse;
import com.alvar.oasisclub.events.dto.EventResponse;
import com.alvar.oasisclub.events.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
@AllArgsConstructor
@Tag(name = "Eventos", description = "Gestión de eventos del club, inscripciones y cancelaciones")
public class EventController {

  private final EventService eventService;
  private final AccessControlService accessControl;

  @GetMapping
  @Operation(
      summary = "Listar eventos activos",
      description = "Devuelve los eventos activos y públicos del club. Si el usuario está autenticado, también indica si ya está inscrito en cada evento."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lista de eventos activos obtenida correctamente")
  })
  public List<EventResponse> getActiveEvents(Authentication authentication) {
    UUID clientId = extractClientId(authentication);
    return eventService.getActiveEvents(clientId);
  }

  @GetMapping("/all")
  @Operation(
      summary = "Listar todos los eventos (administrador)",
      description = "Devuelve todos los eventos del club incluyendo los no activos o pasados. Solo accesible por administradores."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lista completa de eventos obtenida correctamente"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "403", description = "Solo los administradores pueden ver todos los eventos")
  })
  public List<EventResponse> getAllEvents(Authentication authentication) {
    accessControl.requireAdmin(authentication);
    return eventService.getAllEvents();
  }

  @PostMapping
  @Operation(
      summary = "Crear evento",
      description = "Crea un nuevo evento en el club. Solo accesible por administradores."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Evento creado correctamente"),
      @ApiResponse(responseCode = "400", description = "Datos del evento inválidos"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "403", description = "Solo los administradores pueden crear eventos")
  })
  public ResponseEntity<EventResponse> createEvent(
      @Valid @RequestBody CreateEventRequest request,
      Authentication authentication
  ) {
    accessControl.requireAdmin(authentication);
    EventResponse response = eventService.createEvent(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Eliminar evento",
      description = "Elimina un evento del sistema. Esto también cancela todas las inscripciones asociadas. Solo accesible por administradores."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Evento eliminado correctamente"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "403", description = "Solo los administradores pueden eliminar eventos"),
      @ApiResponse(responseCode = "404", description = "Evento no encontrado")
  })
  public ResponseEntity<Void> deleteEvent(
      @Parameter(description = "ID UUID del evento") @PathVariable UUID id,
      Authentication authentication
  ) {
    accessControl.requireAdmin(authentication);
    eventService.deleteEvent(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/registrations")
  @Operation(
      summary = "Listar inscritos en un evento",
      description = "Devuelve la lista de clientes inscritos en un evento específico. Solo accesible por administradores."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lista de inscritos obtenida correctamente"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "403", description = "Solo los administradores pueden ver los inscritos"),
      @ApiResponse(responseCode = "404", description = "Evento no encontrado")
  })
  public List<EventRegistrationResponse> getRegistrations(
      @Parameter(description = "ID UUID del evento") @PathVariable UUID id,
      Authentication authentication
  ) {
    accessControl.requireAdmin(authentication);
    return eventService.getRegistrations(id);
  }

  @DeleteMapping("/{id}/registrations/{regId}")
  @Operation(
      summary = "Eliminar inscripción (administrador)",
      description = "Elimina la inscripción de un cliente en un evento. Solo accesible por administradores."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Inscripción eliminada correctamente"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "403", description = "Solo los administradores pueden eliminar inscripciones"),
      @ApiResponse(responseCode = "404", description = "Evento o inscripción no encontrada")
  })
  public ResponseEntity<Void> removeRegistration(
      @Parameter(description = "ID UUID del evento") @PathVariable UUID id,
      @Parameter(description = "ID UUID de la inscripción") @PathVariable UUID regId,
      Authentication authentication
  ) {
    accessControl.requireAdmin(authentication);
    eventService.adminRemoveRegistration(id, regId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/register")
  @Operation(
      summary = "Inscribirse en un evento",
      description = "Inscribe al cliente autenticado en el evento indicado. Si el evento tiene aforo limitado, se validará la disponibilidad de plazas."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Inscripción realizada correctamente"),
      @ApiResponse(responseCode = "400", description = "El cliente ya está inscrito o no hay plazas disponibles"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "404", description = "Evento no encontrado")
  })
  public ResponseEntity<Void> register(
      @Parameter(description = "ID UUID del evento") @PathVariable UUID id,
      Authentication authentication
  ) {
    AuthenticatedUser user = accessControl.requireUser(authentication);
    eventService.register(id, user.clientId());
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @DeleteMapping("/{id}/register")
  @Operation(
      summary = "Cancelar inscripción propia",
      description = "Cancela la inscripción del cliente autenticado en el evento indicado."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Inscripción cancelada correctamente"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "404", description = "Evento o inscripción no encontrada")
  })
  public ResponseEntity<Void> unregister(
      @Parameter(description = "ID UUID del evento") @PathVariable UUID id,
      Authentication authentication
  ) {
    AuthenticatedUser user = accessControl.requireUser(authentication);
    eventService.unregister(id, user.clientId());
    return ResponseEntity.noContent().build();
  }

  private UUID extractClientId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    try {
      return accessControl.requireUser(authentication).clientId();
    } catch (Exception e) {
      return null;
    }
  }
}
