package com.gestioncaravana.adapter.in.web;

import java.util.List;

public record CaravanFeatCatalogItemResponse(
    String code,
    String name,
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
