package com.gestioncaravana.domain;

import java.time.Instant;
import java.util.UUID;

public record CaravanCargo(
    UUID id,
    UUID caravanId,
    CaravanCargoSourceType sourceType,
    String catalogCode,
    String displayName,
    String category,
    int quantity,
    int cargoUnits,
    Integer currentProvisions,
    Boolean dayPassed,
    UUID wagonId,
    String origin,
    String specificCommodity,
    String deity,
    String notes,
    Instant createdAt,
    Instant updatedAt) {

  public CaravanCargo {
    if (id == null) {
      throw new IllegalArgumentException("id is required");
    }
    if (caravanId == null) {
      throw new IllegalArgumentException("caravanId is required");
    }
    if (sourceType == null) {
      throw new IllegalArgumentException("sourceType is required");
    }
    if (displayName == null || displayName.isBlank()) {
      throw new IllegalArgumentException("displayName is required");
    }
    if (category == null || category.isBlank()) {
      throw new IllegalArgumentException("category is required");
    }
    if (quantity < 1) {
      throw new IllegalArgumentException("quantity must be greater than or equal to 1");
    }
    if (cargoUnits < 1) {
      throw new IllegalArgumentException("cargoUnits must be greater than or equal to 1");
    }
    if (currentProvisions != null && currentProvisions < 0) {
      throw new IllegalArgumentException("currentProvisions must be greater than or equal to 0");
    }
    if (dayPassed == null) {
      throw new IllegalArgumentException("dayPassed is required");
    }
    if (createdAt == null || updatedAt == null) {
      throw new IllegalArgumentException("timestamps are required");
    }
  }

  public static CaravanCargo create(
      UUID id,
      UUID caravanId,
      CaravanCargoSourceType sourceType,
      String catalogCode,
      String displayName,
      String category,
      int quantity,
      int cargoUnits,
      UUID wagonId,
      String origin,
      String specificCommodity,
      String deity,
      String notes,
      Instant now) {
    return new CaravanCargo(
        id,
        caravanId,
        sourceType,
        catalogCode,
        displayName.trim(),
        category.trim(),
        quantity,
        cargoUnits,
        initialProvisionsFor(catalogCode, quantity, cargoUnits),
        false,
        wagonId,
        normalize(origin),
        normalize(specificCommodity),
        normalize(deity),
        normalize(notes),
        now,
        now);
  }

  public CaravanCargo assignWagon(UUID wagonId, Instant now) {
    return new CaravanCargo(
        id,
        caravanId,
        sourceType,
        catalogCode,
        displayName,
        category,
        quantity,
        cargoUnits,
        currentProvisions,
        dayPassed,
        wagonId,
        origin,
        specificCommodity,
        deity,
        notes,
        createdAt,
        now);
  }

  public CaravanCargo update(
      String displayName,
      String category,
      Integer quantity,
      Integer cargoUnits,
      String origin,
      String specificCommodity,
      String deity,
      String notes,
      Instant now) {
    return new CaravanCargo(
        id,
        caravanId,
        sourceType,
        catalogCode,
        displayName == null || displayName.isBlank() ? this.displayName : displayName.trim(),
        category == null || category.isBlank() ? this.category : category.trim(),
        quantity == null ? this.quantity : quantity,
        cargoUnits == null ? this.cargoUnits : cargoUnits,
        currentProvisions,
        dayPassed,
        wagonId,
        origin == null ? this.origin : normalize(origin),
        specificCommodity == null ? this.specificCommodity : normalize(specificCommodity),
        deity == null ? this.deity : normalize(deity),
        notes == null ? this.notes : normalize(notes),
        createdAt,
        now);
  }

  public CaravanCargo withCurrentProvisions(Integer currentProvisions, Boolean dayPassed, Instant now) {
    return new CaravanCargo(
        id,
        caravanId,
        sourceType,
        catalogCode,
        displayName,
        category,
        quantity,
        cargoUnits,
        currentProvisions,
        dayPassed,
        wagonId,
        origin,
        specificCommodity,
        deity,
        notes,
        createdAt,
        now);
  }

  public CaravanCargo withDayPassed(Boolean dayPassed, Instant now) {
    return new CaravanCargo(
        id,
        caravanId,
        sourceType,
        catalogCode,
        displayName,
        category,
        quantity,
        cargoUnits,
        currentProvisions,
        dayPassed,
        wagonId,
        origin,
        specificCommodity,
        deity,
        notes,
        createdAt,
        now);
  }

  private static Integer initialProvisionsFor(String catalogCode, int quantity, int cargoUnits) {
    if ("suministros".equals(catalogCode) || "suministros-perecederos".equals(catalogCode)) {
      return quantity * 10;
    }
    return null;
  }

  private static String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
