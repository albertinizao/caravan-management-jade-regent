package com.gestioncaravana.adapter.in.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CaravanWagonResponse(
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
    List<CaravanBeastResponse> draftBeasts,
    int draftStrength,
    int draftRequiredStrength,
    List<CaravanWagonImprovementResponse> improvements,
    Instant createdAt,
    Instant updatedAt) {}
