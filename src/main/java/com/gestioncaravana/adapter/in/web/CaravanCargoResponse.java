package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.domain.CaravanCargoSourceType;
import java.time.Instant;
import java.util.UUID;

public record CaravanCargoResponse(
    UUID id,
    UUID caravanId,
    CaravanCargoSourceType sourceType,
    String sourceTypeLabel,
    String catalogCode,
    String catalogName,
    String displayName,
    String category,
    int quantity,
    int cargoUnits,
    UUID wagonId,
    String wagonName,
    String origin,
    String specificCommodity,
    String deity,
    String notes,
    String priceExpression,
    Instant createdAt,
    Instant updatedAt) {}
