package com.gestioncaravana.application.model;

import java.util.List;
import java.util.UUID;

public record CaravanDayPreviewView(
    UUID caravanId,
    int dayIndex,
    int currentReserve,
    List<CaravanSupplyConsumptionView> initialProvisionsInConsumption,
    List<CaravanSupplyConsumptionView> provisionsInConsumption,
    int expectedConsumption,
    int expectedGeneration,
    int expectedNetDelta,
    int expectedReserveAfterResolution,
    int expectedShortage,
    int generatedProvisions,
    int generatedFood,
    int consumedProvisions,
    int surplusProvisions,
    List<String> warnings,
    List<CaravanDailyChoiceView> requiredChoices,
    List<CaravanDailyContributionView> contributions,
    String cargoMovementSummary) {}
