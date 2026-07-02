package com.gestioncaravana.application.model;

import java.util.List;
import java.util.UUID;

public record CaravanDayPreviewView(
    UUID caravanId,
    int dayIndex,
    int currentReserve,
    int expectedConsumption,
    int expectedGeneration,
    int expectedNetDelta,
    int expectedReserveAfterResolution,
    int expectedShortage,
    List<String> warnings,
    List<CaravanDailyChoiceView> requiredChoices,
    List<CaravanDailyContributionView> contributions) {}
