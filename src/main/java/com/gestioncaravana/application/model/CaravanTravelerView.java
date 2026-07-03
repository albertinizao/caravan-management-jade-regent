package com.gestioncaravana.application.model;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CaravanTravelerView(
    UUID id,
    UUID caravanId,
    String fullName,
    String description,
    List<String> availableRoleCodes,
    List<String> activeRoleCodes,
    String activeRoleCode,
    String activeRoleName,
    UUID wagonId,
    String wagonName,
    UUID drivingWagonId,
    String drivingWagonName,
    int maxActiveRoleCount,
    BigDecimal salary,
    String contractConditions,
    int consumption,
    UUID servedTravelerId,
    String servedTravelerName,
    Instant createdAt,
    Instant updatedAt) {}
