package com.gestioncaravana.application.model;

public record CaravanOtherStatsView(
    int speed,
    int travelerCapacity,
    int cargoCapacity,
    int cargoLoad,
    int cargoRemaining,
    int consumption,
    int travelerCount,
    int wagonCount,
    int beastCount,
    int maxWagons) {}
