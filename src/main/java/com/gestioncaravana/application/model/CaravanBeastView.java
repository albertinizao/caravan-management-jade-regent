package com.gestioncaravana.application.model;

import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import com.gestioncaravana.domain.CaravanBeastSourceType;
import java.time.Instant;
import java.util.UUID;

public record CaravanBeastView(
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
    Instant updatedAt) {}
