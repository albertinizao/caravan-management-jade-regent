package com.gestioncaravana.application.model;

import com.gestioncaravana.domain.CaravanFeatAcquisitionSourceType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CaravanFeatView(
    UUID id,
    UUID caravanId,
    String featTypeCode,
    String name,
    String description,
    List<String> prerequisites,
    String benefitText,
    String specialText,
    String notes,
    CaravanFeatAcquisitionSourceType acquisitionSourceType,
    Integer acquisitionLevel,
    String acquisitionCause,
    int selectionIndex,
    boolean active,
    String blockedReason,
    Boolean manualApplies,
    String manualAppliesReason,
    String automationMode,
    String automationStateInputs,
    String automationExactAutomation,
    Instant createdAt,
    Instant updatedAt) {}
