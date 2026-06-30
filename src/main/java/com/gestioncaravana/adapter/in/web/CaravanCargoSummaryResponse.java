package com.gestioncaravana.adapter.in.web;

import java.util.UUID;

public record CaravanCargoSummaryResponse(
    UUID wagonId,
    String wagonName,
    int cargoCapacity,
    int usedCargoUnits,
    int remainingCargoUnits,
    int cargoEntryCount) {}
