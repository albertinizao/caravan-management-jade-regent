package com.gestioncaravana.domain;

import java.time.Instant;
import java.util.UUID;

public record CaravanWagon(
    UUID id,
    UUID caravanId,
    String wagonTypeCode,
    String displayName,
    String specificCommodity,
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
    displayName = normalize(displayName);
    specificCommodity = normalize(specificCommodity);
    if (createdAt == null || updatedAt == null) {
      throw new IllegalArgumentException("timestamps are required");
    }
  }

  public static CaravanWagon create(UUID id, UUID caravanId, String wagonTypeCode, String displayName, Instant now) {
    return create(id, caravanId, wagonTypeCode, displayName, null, now);
  }

  public static CaravanWagon create(
      UUID id, UUID caravanId, String wagonTypeCode, String displayName, String specificCommodity, Instant now) {
    return new CaravanWagon(id, caravanId, wagonTypeCode, displayName, specificCommodity, now, now);
  }

  public CaravanWagon rename(String displayName, Instant now) {
    return new CaravanWagon(id, caravanId, wagonTypeCode, displayName, specificCommodity, createdAt, now);
  }

  public String displayNameOr(String fallback) {
    return displayName == null || displayName.isBlank() ? fallback : displayName;
  }

  public String specificCommodityOr(String fallback) {
    return specificCommodity == null || specificCommodity.isBlank() ? fallback : specificCommodity;
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    var trimmed = value.trim();
    return trimmed.isBlank() ? null : trimmed;
  }
}
