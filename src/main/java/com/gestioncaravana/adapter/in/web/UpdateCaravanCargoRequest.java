package com.gestioncaravana.adapter.in.web;

import java.util.UUID;

public record UpdateCaravanCargoRequest(
    String displayName,
    String category,
    Integer quantity,
    Integer cargoUnits,
    UUID wagonId,
    String origin,
    String specificCommodity,
    String deity,
    String notes) {}
