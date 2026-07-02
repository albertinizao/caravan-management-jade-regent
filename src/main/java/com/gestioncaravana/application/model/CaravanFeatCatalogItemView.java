package com.gestioncaravana.application.model;

import java.util.List;

public record CaravanFeatCatalogItemView(
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
    int ownedCount,
    boolean available,
    String blockedReason) {}
