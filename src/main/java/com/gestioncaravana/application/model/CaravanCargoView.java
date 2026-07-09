package com.gestioncaravana.application.model;

import com.gestioncaravana.domain.CaravanCargoSourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CaravanCargoView(
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
    BigDecimal currentProvisions,
    Boolean dayPassed,
    String priceExpression,
    Instant createdAt,
    Instant updatedAt) {}
