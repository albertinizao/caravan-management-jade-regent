package com.gestioncaravana.adapter.in.web;

public record CaravanStatContributionResponse(
    String statCode,
    String sourceType,
    String sourceId,
    String sourceName,
    String modifier,
    String operation,
    String reason) {}
