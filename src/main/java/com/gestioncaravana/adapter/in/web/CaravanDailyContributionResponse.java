package com.gestioncaravana.adapter.in.web;

record CaravanDailyContributionResponse(
    String effectCode,
    String sourceType,
    String sourceId,
    String sourceName,
    String operation,
    int quantity,
    String reason,
    boolean applied,
    String ignoredReason) {}
