package com.gestioncaravana.adapter.in.web;

import java.util.List;

public record CaravanFeatCatalogItemResponse(
    String code,
    String name,
    String description,
    List<String> prerequisites,
    String benefitText,
    String specialText,
    String notes,
    boolean repeatable,
    int selectionLimit,
    Integer minimumLevel,
    String automationMode,
    String automationStateInputs,
    String automationExactAutomation,
    int ownedCount,
    boolean available,
    String blockedReason) {}
