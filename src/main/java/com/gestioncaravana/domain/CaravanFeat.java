package com.gestioncaravana.domain;

import java.time.Instant;
import java.util.UUID;

public record CaravanFeat(
    UUID id,
    UUID caravanId,
    String featTypeCode,
    CaravanFeatAcquisitionSourceType acquisitionSourceType,
    Integer acquisitionLevel,
    String acquisitionCause,
    int selectionIndex,
    boolean active,
    Boolean manualApplies,
    String manualAppliesReason,
    Instant createdAt,
    Instant updatedAt) {

  public CaravanFeat {
    featTypeCode = normalizeRequired(featTypeCode, "featTypeCode");
    acquisitionCause = normalize(acquisitionCause);
    if (id == null) {
      throw new IllegalArgumentException("id is required");
    }
    if (caravanId == null) {
      throw new IllegalArgumentException("caravanId is required");
    }
    if (acquisitionSourceType == null) {
      throw new IllegalArgumentException("acquisitionSourceType is required");
    }
    if (acquisitionSourceType == CaravanFeatAcquisitionSourceType.LEVEL_UP) {
      if (acquisitionLevel == null || acquisitionLevel < 1) {
        throw new IllegalArgumentException("acquisitionLevel is required for level-up feats");
      }
      if (acquisitionCause != null) {
        throw new IllegalArgumentException("acquisitionCause must be empty for level-up feats");
      }
    }
    if (acquisitionSourceType == CaravanFeatAcquisitionSourceType.OTHER) {
      if (acquisitionCause == null) {
        throw new IllegalArgumentException("acquisitionCause is required for non-level-up feats");
      }
      if (acquisitionLevel != null) {
        throw new IllegalArgumentException("acquisitionLevel must be empty for non-level-up feats");
      }
    }
    if (selectionIndex < 1) {
      throw new IllegalArgumentException("selectionIndex must be greater than or equal to 1");
    }
    if (manualApplies == null && manualAppliesReason != null) {
      throw new IllegalArgumentException("manualAppliesReason requires manualApplies");
    }
    manualAppliesReason = normalize(manualAppliesReason);
    if (createdAt == null || updatedAt == null) {
      throw new IllegalArgumentException("timestamps are required");
    }
  }

  public static CaravanFeat create(
      UUID id,
      UUID caravanId,
      String featTypeCode,
      CaravanFeatAcquisitionSourceType acquisitionSourceType,
      Integer acquisitionLevel,
      String acquisitionCause,
      int selectionIndex,
      Instant now) {
    return create(
        id,
        caravanId,
        featTypeCode,
        acquisitionSourceType,
        acquisitionLevel,
        acquisitionCause,
        selectionIndex,
        true,
        null,
        null,
        now);
  }

  public static CaravanFeat create(
      UUID id,
      UUID caravanId,
      String featTypeCode,
      CaravanFeatAcquisitionSourceType acquisitionSourceType,
      Integer acquisitionLevel,
      String acquisitionCause,
      int selectionIndex,
      boolean active,
      Boolean manualApplies,
      String manualAppliesReason,
      Instant now) {
    return new CaravanFeat(
        id,
        caravanId,
        featTypeCode,
        acquisitionSourceType,
        resolveAcquisitionLevel(acquisitionSourceType, acquisitionLevel),
        resolveAcquisitionCause(acquisitionSourceType, acquisitionCause),
        selectionIndex,
        active,
        manualApplies,
        manualAppliesReason,
        now,
        now);
  }

  public CaravanFeat updateAcquisition(
      CaravanFeatAcquisitionSourceType sourceType,
      Integer acquisitionLevel,
      String acquisitionCause,
      Boolean active,
      Boolean manualApplies,
      String manualAppliesReason,
      Instant now) {
    return new CaravanFeat(
        id,
        caravanId,
        featTypeCode,
        sourceType,
        resolveAcquisitionLevel(sourceType, acquisitionLevel),
        resolveAcquisitionCause(sourceType, acquisitionCause),
        selectionIndex,
        active == null ? this.active : active,
        manualApplies == null ? this.manualApplies : manualApplies,
        manualAppliesReason == null ? this.manualAppliesReason : manualAppliesReason,
        createdAt,
        now);
  }

  private static Integer resolveAcquisitionLevel(
      CaravanFeatAcquisitionSourceType sourceType, Integer acquisitionLevel) {
    if (sourceType == CaravanFeatAcquisitionSourceType.OTHER) {
      return null;
    }
    return acquisitionLevel;
  }

  private static String resolveAcquisitionCause(
      CaravanFeatAcquisitionSourceType sourceType, String acquisitionCause) {
    if (sourceType == CaravanFeatAcquisitionSourceType.LEVEL_UP) {
      return null;
    }
    return normalize(acquisitionCause);
  }

  private static String normalizeRequired(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }

  private static String normalize(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    return text.trim();
  }
}
