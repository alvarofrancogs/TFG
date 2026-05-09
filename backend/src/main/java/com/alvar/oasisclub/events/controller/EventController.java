package com.alvar.oasisclub.events.controller;

import com.alvar.oasisclub.auth.security.AccessControlService;
import com.alvar.oasisclub.auth.security.AuthenticatedUser;
import com.alvar.oasisclub.events.dto.CreateEventRequest;
import com.alvar.oasisclub.events.dto.EventRegistrationResponse;
import com.alvar.oasisclub.events.dto.EventResponse;
import com.alvar.oasisclub.events.service.EventService;
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
public class EventController {

  private final EventService eventService;
  private final AccessControlService accessControl;

  @GetMapping
  public List<EventResponse> getActiveEvents(Authentication authentication) {
    UUID clientId = extractClientId(authentication);
    return eventService.getActiveEvents(clientId);
  }

  @GetMapping("/all")
  public List<EventResponse> getAllEvents(Authentication authentication) {
    accessControl.requireAdmin(authentication);
    return eventService.getAllEvents();
  }

  @PostMapping
  public ResponseEntity<EventResponse> createEvent(
      @Valid @RequestBody CreateEventRequest request,
      Authentication authentication
  ) {
    accessControl.requireAdmin(authentication);
    EventResponse response = eventService.createEvent(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteEvent(
      @PathVariable UUID id,
      Authentication authentication
  ) {
    accessControl.requireAdmin(authentication);
    eventService.deleteEvent(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/registrations")
  public List<EventRegistrationResponse> getRegistrations(
      @PathVariable UUID id,
      Authentication authentication
  ) {
    accessControl.requireAdmin(authentication);
    return eventService.getRegistrations(id);
  }

  @DeleteMapping("/{id}/registrations/{regId}")
  public ResponseEntity<Void> removeRegistration(
      @PathVariable UUID id,
      @PathVariable UUID regId,
      Authentication authentication
  ) {
    accessControl.requireAdmin(authentication);
    eventService.adminRemoveRegistration(id, regId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/register")
  public ResponseEntity<Void> register(
      @PathVariable UUID id,
      Authentication authentication
  ) {
    AuthenticatedUser user = accessControl.requireUser(authentication);
    eventService.register(id, user.clientId());
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @DeleteMapping("/{id}/register")
  public ResponseEntity<Void> unregister(
      @PathVariable UUID id,
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
