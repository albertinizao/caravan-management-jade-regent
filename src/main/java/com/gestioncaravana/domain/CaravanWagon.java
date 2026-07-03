package com.gestioncaravana.domain;

import java.time.Instant;
import java.util.UUID;

public record CaravanWagon(
    UUID id,
    UUID caravanId,
    String wagonTypeCode,
    String displayName,
    String specificCommodity,
    Integer currentHitPoints,
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
    if (currentHitPoints != null && currentHitPoints < 0) {
      throw new IllegalArgumentException("currentHitPoints must be greater than or equal to 0");
    }
    if (createdAt == null || updatedAt == null) {
      throw new IllegalArgumentException("timestamps are required");
    }
  }

  public static CaravanWagon create(UUID id, UUID caravanId, String wagonTypeCode, String displayName, Instant now) {
    return create(id, caravanId, wagonTypeCode, displayName, null, null, now);
  }

  public static CaravanWagon create(
      UUID id, UUID caravanId, String wagonTypeCode, String displayName, String specificCommodity, Instant now) {
    return create(id, caravanId, wagonTypeCode, displayName, specificCommodity, null, now);
  }

  public static CaravanWagon create(
      UUID id,
      UUID caravanId,
      String wagonTypeCode,
      String displayName,
      String specificCommodity,
      Integer currentHitPoints,
      Instant now) {
    return new CaravanWagon(id, caravanId, wagonTypeCode, displayName, specificCommodity, currentHitPoints, now, now);
  }

  public CaravanWagon rename(String displayName, Instant now) {
    return new CaravanWagon(id, caravanId, wagonTypeCode, displayName, specificCommodity, currentHitPoints, createdAt, now);
  }

  public CaravanWagon withCurrentHitPoints(Integer currentHitPoints, Instant now) {
    return new CaravanWagon(id, caravanId, wagonTypeCode, displayName, specificCommodity, currentHitPoints, createdAt, now);
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
