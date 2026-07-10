package com.gestioncaravana.application.model;

import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import com.gestioncaravana.domain.CaravanBeastSourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
    int consumption,
    List<String> availableRoleCodes,
    String activeRoleCode,
    CaravanBeastAssignmentType assignmentType,
    UUID assignedWagonId,
    String assignedWagonName,
    Instant createdAt,
    Instant updatedAt,
    BigDecimal occupiedSpace) {}
