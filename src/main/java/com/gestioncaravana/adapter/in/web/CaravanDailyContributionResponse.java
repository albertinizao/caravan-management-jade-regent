package com.gestioncaravana.adapter.in.web;

record CaravanDailyContributionResponse(
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
