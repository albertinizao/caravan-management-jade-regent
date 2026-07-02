package com.gestioncaravana.adapter.in.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record CaravanDayCycleResponse(
    UUID caravanId,
    String idempotencyKey,
    int dayIndex,
    int currentReserve,
    int expectedConsumption,
    int expectedGeneration,
    int expectedNetDelta,
    int expectedReserveAfterResolution,
    int shortage,
    Instant resolvedAt,
    List<CaravanDailyChoiceResponse> choices,
    List<CaravanDailyContributionResponse> contributions,
    List<String> warnings) {}
