package com.gestioncaravana.application.model;

import com.gestioncaravana.domain.CaravanCampaignStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CaravanCampaignView(
    UUID id,
    String name,
    String description,
    int level,
    CaravanMainStatsView mainStats,
    int discontent,
    CaravanCampaignStatus status,
    boolean active,
    Instant createdAt,
    Instant updatedAt,
    List<String> wagons,
    List<String> travelers,
    List<String> beasts,
    List<String> feats) {}

