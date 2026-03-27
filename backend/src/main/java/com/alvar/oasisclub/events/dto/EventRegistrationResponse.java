package com.alvar.oasisclub.events.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventRegistrationResponse(
    UUID id,
    UUID clientId,
    String clientName,
    String clientEmail,
    LocalDateTime registeredAt
) {}
