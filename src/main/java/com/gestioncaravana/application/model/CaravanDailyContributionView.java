package com.gestioncaravana.application.model;

public record CaravanDailyContributionView(
    String effectCode,
    String sourceType,
    String sourceId,
    String sourceName,
    String operation,
    int quantity,
    String reason,
    boolean applied,
    String ignoredReason) {}
