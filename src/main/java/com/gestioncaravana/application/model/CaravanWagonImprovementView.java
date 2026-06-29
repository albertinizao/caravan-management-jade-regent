package com.gestioncaravana.application.model;

import java.time.Instant;
import java.util.UUID;

public record CaravanWagonImprovementView(
    UUID id,
    UUID caravanId,
    UUID wagonId,
    String improvementTypeCode,
    String name,
    String category,
    String costExpression,
    String specialBenefit,
    String description,
    String notes,
    Instant createdAt,
    Instant updatedAt) {}
