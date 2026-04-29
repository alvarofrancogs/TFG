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
}