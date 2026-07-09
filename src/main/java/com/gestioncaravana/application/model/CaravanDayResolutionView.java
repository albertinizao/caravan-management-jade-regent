package com.gestioncaravana.application.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CaravanDayResolutionView(
    UUID id,
    UUID caravanId,
    String idempotencyKey,
    int resolvedDayIndex,
    Instant resolvedAt,
    int startingReserve,
    List<CaravanSupplyConsumptionView> initialProvisionsInConsumption,
    List<CaravanSupplyConsumptionView> provisionsInConsumption,
    int endingReserve,
    int totalConsumption,
    int totalGeneration,
    int netDelta,
    int shortage,
    int generatedProvisions,
    int generatedFood,
    int consumedProvisions,
    int surplusProvisions,
    List<CaravanDailyChoiceView> choices,
    List<CaravanDailyContributionView> contributions,
    List<String> warnings,
    String cargoMovementSummary) {}
