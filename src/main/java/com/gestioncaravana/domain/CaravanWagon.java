package com.gestioncaravana.domain;

import java.time.Instant;
import java.util.UUID;

public record CaravanWagon(
    UUID id,
    UUID caravanId,
    String wagonTypeCode,
    Instant createdAt,
    Instant updatedAt) {

  public CaravanWagon {
    if (id == null) {
      throw new IllegalArgumentException("id is required");
    }
    if (caravanId == null) {
      throw new IllegalArgumentException("caravanId is required");
    }
    if (wagonTypeCode == null || wagonTypeCode.isBlank()) {
      throw new IllegalArgumentException("wagonTypeCode is required");
    }
    if (createdAt == null || updatedAt == null) {
      throw new IllegalArgumentException("timestamps are required");
    }
  }

  public static CaravanWagon create(UUID id, UUID caravanId, String wagonTypeCode, Instant now) {
    return new CaravanWagon(id, caravanId, wagonTypeCode, now, now);
  }
}
