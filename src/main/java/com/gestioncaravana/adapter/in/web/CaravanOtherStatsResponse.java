package com.gestioncaravana.adapter.in.web;

public record CaravanOtherStatsResponse(
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
