package com.gestioncaravana.application.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CaravanDayCyclePreviewView(
    UUID caravanId,
    String previewFingerprint,
    boolean confirmed,
    UUID resolutionId,
    Instant confirmedAt,
    int dayIndex,
    int currentSupplyUnits,
    BigDecimal currentPerishableFood,
    int currentPerishableUnits,
    int generatedSuppliesFromAgricultors,
    BigDecimal generatedAlchemyValueFromBoticarios,
    BigDecimal requiredConsumption,
    boolean consumptionCovered,
    BigDecimal generatedFood,
    BigDecimal leftoverFood,
    int finalSupplyUnits,
    int finalPerishableUnits,
    BigDecimal finalPerishableFood,
    int suppliesConsumed,
    List<CaravanDayCycleLogEntryView> simulation,
    List<String> warnings) {

  public CaravanDayCyclePreviewView withConfirmation(UUID resolutionId, Instant confirmedAt) {
    return new CaravanDayCyclePreviewView(
        caravanId,
        previewFingerprint,
        true,
        resolutionId,
        confirmedAt,
        dayIndex,
        currentSupplyUnits,
        currentPerishableFood,
        currentPerishableUnits,
        generatedSuppliesFromAgricultors,
        generatedAlchemyValueFromBoticarios,
        requiredConsumption,
        consumptionCovered,
        generatedFood,
        leftoverFood,
        finalSupplyUnits,
        finalPerishableUnits,
        finalPerishableFood,
        suppliesConsumed,
        simulation,
        warnings);
  }
}
