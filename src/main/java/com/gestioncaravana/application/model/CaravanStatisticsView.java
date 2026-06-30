package com.gestioncaravana.application.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CaravanStatisticsView(
    UUID caravanId,
    int level,
    CaravanMainStatsView mainStats,
    CaravanDerivedStatsView derivedStats,
    CaravanOtherStatsView otherStats,
    int discontent,
    int moraleThreshold,
    List<CaravanStatContributionView> contributions,
    List<String> warnings,
    Instant updatedAt) {}
