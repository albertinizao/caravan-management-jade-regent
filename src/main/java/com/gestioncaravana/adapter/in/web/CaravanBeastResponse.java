package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import com.gestioncaravana.domain.CaravanBeastSourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CaravanBeastResponse(
    UUID id,
    UUID caravanId,
    CaravanBeastSourceType sourceType,
    String catalogBeastCode,
    String name,
    String size,
    int strength,
    int speed,
    Integer thermalAdaptation,
    Integer basePrice,
    Integer trainedPrice,
    boolean fourLegged,
    String specialNote,
    String description,
    String customNotes,
    CaravanBeastAssignmentType assignmentType,
    UUID assignedWagonId,
    String assignedWagonName,
    Instant createdAt,
    Instant updatedAt,
    BigDecimal occupiedSpace) {}
