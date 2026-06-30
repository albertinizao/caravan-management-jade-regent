package com.gestioncaravana.adapter.in.web;

import java.time.Instant;
import java.util.UUID;

public record CaravanWagonImprovementResponse(
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
