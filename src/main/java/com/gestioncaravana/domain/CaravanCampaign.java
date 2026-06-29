package com.gestioncaravana.domain;

import java.time.Instant;
import java.util.UUID;

public record CaravanCampaign(
    UUID id,
    String name,
    String description,
    int level,
    CaravanMainStats mainStats,
    int discontent,
    CaravanCampaignStatus status,
    Instant createdAt,
    Instant updatedAt) {

  public CaravanCampaign {
    if (id == null) {
      throw new IllegalArgumentException("id is required");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
    if (level < 1) {
      throw new IllegalArgumentException("level must be greater than or equal to 1");
    }
    if (mainStats == null) {
      throw new IllegalArgumentException("mainStats is required");
    }
    if (discontent < 0) {
      throw new IllegalArgumentException("discontent must be greater than or equal to 0");
    }
    if (status == null) {
      throw new IllegalArgumentException("status is required");
    }
    if (createdAt == null || updatedAt == null) {
      throw new IllegalArgumentException("timestamps are required");
    }
  }

  public static CaravanCampaign create(UUID id, String name, String description, Instant now) {
    return new CaravanCampaign(
        id,
        name.trim(),
        normalizeDescription(description),
        1,
        CaravanMainStats.initial(),
        0,
        CaravanCampaignStatus.ACTIVE,
        now,
        now);
  }

  public CaravanCampaign markSelectedAt(Instant now) {
    return new CaravanCampaign(id, name, description, level, mainStats, discontent, status, createdAt, now);
  }

  private static String normalizeDescription(String description) {
    if (description == null || description.isBlank()) {
      return null;
    }
    return description.trim();
  }
}

