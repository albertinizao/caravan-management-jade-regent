package com.gestioncaravana.application.model;

public record CaravanDailyContributionView(
    String effectCode,
    String sourceType,
    String sourceId,
    String sourceName,
    String sourceRoleName,
    String operation,
    int quantity,
    String quantityUnit,
    String reason,
    boolean applied,
    String ignoredReason) {}
