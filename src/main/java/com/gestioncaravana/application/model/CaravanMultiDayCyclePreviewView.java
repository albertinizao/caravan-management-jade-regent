package com.gestioncaravana.application.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public record CaravanMultiDayCyclePreviewView(
    UUID caravanId,
    String basePreviewFingerprint,
    boolean confirmed,
    int requestedDays,
    int startDayIndex,
    int endDayIndex,
    BigDecimal totalRequiredConsumption,
    BigDecimal totalGeneratedFood,
    int totalGeneratedSuppliesFromAgricultors,
    BigDecimal totalGeneratedAlchemyValueFromBoticarios,
    int totalSuppliesConsumed,
    int daysWithUncoveredConsumption,
    int finalSupplyUnits,
    int finalPerishableUnits,
    BigDecimal finalPerishableFood,
    Instant confirmedAt,
    List<String> warnings,
    List<CaravanDayCyclePreviewView> dayPreviews) {

  public CaravanMultiDayCyclePreviewView {
    if (caravanId == null) {
      throw new IllegalArgumentException("caravanId is required");
    }
    if (basePreviewFingerprint == null || basePreviewFingerprint.isBlank()) {
      throw new IllegalArgumentException("basePreviewFingerprint is required");
    }
    if (requestedDays < 1) {
      throw new IllegalArgumentException("requestedDays must be greater than or equal to 1");
    }
    if (startDayIndex < 1) {
      throw new IllegalArgumentException("startDayIndex must be greater than or equal to 1");
    }
    if (endDayIndex < startDayIndex) {
      throw new IllegalArgumentException("endDayIndex must be greater than or equal to startDayIndex");
    }
    if (totalRequiredConsumption == null
        || totalGeneratedFood == null
        || totalGeneratedAlchemyValueFromBoticarios == null
        || finalPerishableFood == null) {
      throw new IllegalArgumentException("aggregated numeric values are required");
    }
    if (warnings == null) {
      throw new IllegalArgumentException("warnings are required");
    }
    if (dayPreviews == null || dayPreviews.isEmpty()) {
      throw new IllegalArgumentException("dayPreviews are required");
    }
    warnings = List.copyOf(new LinkedHashSet<>(warnings));
    dayPreviews = List.copyOf(dayPreviews);
  }
}
