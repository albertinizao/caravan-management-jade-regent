package com.gestioncaravana.application.model;

public record CaravanStatContributionView(
    String statCode,
    String sourceType,
    String sourceId,
    String sourceName,
    String modifier,
    String operation,
    String reason) {}
