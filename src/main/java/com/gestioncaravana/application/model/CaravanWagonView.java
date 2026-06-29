package com.gestioncaravana.application.model;

import java.time.Instant;
import java.util.UUID;

public record CaravanWagonView(
    UUID id,
    UUID caravanId,
    String wagonTypeCode,
    String name,
    String category,
    int cost,
    int hitPoints,
    int hardness,
    String propulsion,
    int travelerCapacity,
    int cargoCapacity,
    String limitKind,
    Integer limitFixedMax,
    Integer limitRatioDenominator,
    String limit,
    int consumption,
    String specialBenefit,
    String description,
    String notes,
    Instant createdAt,
    Instant updatedAt) {}
