package com.gestioncaravana.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record AddCustomCargoRequest(
    @NotBlank String displayName,
    @NotBlank String category,
    Integer quantity,
    Integer cargoUnits,
    UUID wagonId,
    String origin,
    String specificCommodity,
    String deity,
    String notes) {}
