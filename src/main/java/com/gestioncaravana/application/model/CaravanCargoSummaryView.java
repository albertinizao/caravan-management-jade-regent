package com.gestioncaravana.application.model;

import java.util.UUID;

public record CaravanCargoSummaryView(
    UUID wagonId,
    String wagonName,
    int cargoCapacity,
    int usedCargoUnits,
    int remainingCargoUnits,
    int cargoEntryCount) {}
