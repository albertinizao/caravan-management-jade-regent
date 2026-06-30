package com.gestioncaravana.adapter.in.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CaravanStatisticsResponse(
    UUID caravanId,
    int level,
    CaravanMainStatsResponse mainStats,
    CaravanDerivedStatsResponse derivedStats,
    CaravanOtherStatsResponse otherStats,
    int discontent,
    int moraleThreshold,
    List<CaravanStatContributionResponse> contributions,
    List<String> warnings,
    Instant updatedAt) {}
