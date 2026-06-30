package com.gestioncaravana.domain;

import java.time.Instant;
import java.util.UUID;

public record CaravanWagonImprovement(
    UUID id,
    UUID caravanId,
    UUID wagonId,
    String improvementTypeCode,
    Instant createdAt,
    Instant updatedAt) {

  public CaravanWagonImprovement {
    if (id == null) {
      throw new IllegalArgumentException("id is required");
    }
    if (caravanId == null) {
      throw new IllegalArgumentException("caravanId is required");
    }
    if (wagonId == null) {
      throw new IllegalArgumentException("wagonId is required");
    }
    if (improvementTypeCode == null || improvementTypeCode.isBlank()) {
      throw new IllegalArgumentException("improvementTypeCode is required");
    }
    if (createdAt == null || updatedAt == null) {
      throw new IllegalArgumentException("timestamps are required");
    }
  }

  public static CaravanWagonImprovement create(UUID id, UUID caravanId, UUID wagonId, String improvementTypeCode, Instant now) {
    return new CaravanWagonImprovement(id, caravanId, wagonId, improvementTypeCode, now, now);
  }
}
