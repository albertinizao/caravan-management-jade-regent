package com.gestioncaravana.domain;

import java.time.Instant;
import java.util.UUID;

public record CustomCalendarEvent(
    Long id,
    UUID caravanId,
    GolarionDate date,
    String name,
    String description,
    boolean secret,
    Instant createdAt) {}
