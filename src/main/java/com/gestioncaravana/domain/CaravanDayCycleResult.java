package com.gestioncaravana.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CaravanDayCycleResult(
    UUID id,
    UUID caravanId,
    String previewFingerprint,
    int dayIndex,
    Instant resolvedAt,
    int startingSupplyUnits,
    BigDecimal startingPerishableFood,
    int startingPerishableUnits,
    int generatedSuppliesFromAgricultors,
    BigDecimal generatedAlchemyValueFromBoticarios,
    BigDecimal requiredConsumption,
    BigDecimal generatedFood,
    BigDecimal leftoverFood,
    int finalSupplyUnits,
    int finalPerishableUnits,
    BigDecimal finalPerishableFood,
    boolean confirmed,
    String simulationJson,
    String warningsJson) {

  public CaravanDayCycleResult {
    if (id == null) {
      throw new IllegalArgumentException("id is required");
    }
    if (caravanId == null) {
      throw new IllegalArgumentException("caravanId is required");
    }
    if (previewFingerprint == null || previewFingerprint.isBlank()) {
      throw new IllegalArgumentException("previewFingerprint is required");
    }
    if (dayIndex < 1) {
      throw new IllegalArgumentException("dayIndex must be greater than or equal to 1");
    }
    if (resolvedAt == null) {
      throw new IllegalArgumentException("resolvedAt is required");
    }
    if (startingSupplyUnits < 0 || startingPerishableUnits < 0 || generatedSuppliesFromAgricultors < 0 || finalSupplyUnits < 0 || finalPerishableUnits < 0) {
      throw new IllegalArgumentException("unit counts must be greater than or equal to 0");
    }
    if (startingPerishableFood == null || generatedAlchemyValueFromBoticarios == null || requiredConsumption == null || generatedFood == null || leftoverFood == null || finalPerishableFood == null) {
      throw new IllegalArgumentException("food values are required");
    }
  }
}
