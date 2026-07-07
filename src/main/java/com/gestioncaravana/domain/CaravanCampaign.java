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
    return create(id, name, description, CaravanMainStats.initial(), now);
  }

  public static CaravanCampaign create(
      UUID id,
      String name,
      String description,
      CaravanMainStats mainStats,
      Instant now) {
    return new CaravanCampaign(
        id,
        name.trim(),
        normalizeDescription(description),
        1,
        mainStats,
        0,
        CaravanCampaignStatus.ACTIVE,
        now,
        now);
  }

  public CaravanCampaign markSelectedAt(Instant now) {
    return new CaravanCampaign(id, name, description, level, mainStats, discontent, status, createdAt, now);
  }

  public CaravanCampaign adjustLevel(int delta, Instant now) {
    if (delta == 0) {
      throw new IllegalArgumentException("delta must not be 0");
    }

    var updatedLevel = level + delta;
    if (updatedLevel < 1) {
      throw new IllegalArgumentException("level must be greater than or equal to 1");
    }

    return new CaravanCampaign(id, name, description, updatedLevel, mainStats, discontent, status, createdAt, now);
  }

  public CaravanCampaign adjustDiscontent(int delta, Instant now) {
    if (delta == 0) {
      throw new IllegalArgumentException("delta must not be 0");
    }

    var updatedDiscontent = discontent + delta;
    if (updatedDiscontent < 0) {
      throw new IllegalArgumentException("discontent must be greater than or equal to 0");
    }

    return new CaravanCampaign(id, name, description, level, mainStats, updatedDiscontent, status, createdAt, now);
  }

  public CaravanCampaign updateMainStats(
      int offense,
      int defense,
      int mobility,
      int morale,
      Instant now) {
    var totalPoints = mainStats.offense()
        + mainStats.defense()
        + mainStats.mobility()
        + mainStats.morale()
        + mainStats.unassignedPoints();
    var updatedMainStats = CaravanMainStats.withUpdatedAllocation(offense, defense, mobility, morale, totalPoints);
    return new CaravanCampaign(id, name, description, level, updatedMainStats, discontent, status, createdAt, now);
  }

  private static String normalizeDescription(String description) {
    if (description == null || description.isBlank()) {
      return null;
    }
    return description.trim();
  }
}

