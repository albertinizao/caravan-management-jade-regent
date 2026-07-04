package com.gestioncaravana.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AddCaravanTravelerRequest(
    @NotBlank String fullName,
    String description,
    @NotEmpty List<String> availableRoleCodes,
    @NotEmpty List<String> activeRoleCodes,
    String activeRoleCode,
    Integer maxActiveRoleCount,
    BigDecimal salary,
    String contractConditions,
    Integer consumption,
    BigDecimal occupiedSpace,
    UUID wagonId,
    UUID drivingWagonId,
    UUID servedTravelerId) {}
