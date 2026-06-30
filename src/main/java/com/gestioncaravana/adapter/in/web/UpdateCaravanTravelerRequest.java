package com.gestioncaravana.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateCaravanTravelerRequest(
    @NotBlank String fullName,
    String description,
    List<String> availableRoleCodes,
    List<String> activeRoleCodes,
    String activeRoleCode,
    Integer maxActiveRoleCount,
    UUID wagonId,
    BigDecimal salary,
    String contractConditions,
    Integer consumption,
    UUID servedTravelerId) {}
