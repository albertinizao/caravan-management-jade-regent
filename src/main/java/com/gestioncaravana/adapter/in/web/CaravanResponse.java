package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.domain.CaravanCampaignStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CaravanResponse(
    UUID id,
    String name,
    String description,
    int level,
    CaravanMainStatsResponse mainStats,
    int discontent,
    CaravanCampaignStatus status,
    boolean active,
    Instant createdAt,
    Instant updatedAt,
    List<String> wagons,
    List<String> travelers,
    List<String> beasts,
    List<String> feats) {}

