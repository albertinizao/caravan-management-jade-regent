package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.domain.CaravanFeatAcquisitionSourceType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCaravanFeatRequest(
    @NotBlank String featTypeCode,
    @NotNull CaravanFeatAcquisitionSourceType acquisitionSourceType,
    @Positive Integer acquisitionLevel,
    String acquisitionCause,
    Boolean active,
    Boolean manualApplies,
    String manualAppliesReason) {

  @AssertTrue(message = "level-up feats require a positive level and other feats require a non-blank cause")
  public boolean hasValidAcquisitionMetadata() {
    if (acquisitionSourceType == null) {
      return false;
    }
    return switch (acquisitionSourceType) {
      case LEVEL_UP -> acquisitionLevel != null && acquisitionLevel > 0 && isBlank(acquisitionCause);
      case OTHER -> acquisitionLevel == null && !isBlank(acquisitionCause);
    };
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
