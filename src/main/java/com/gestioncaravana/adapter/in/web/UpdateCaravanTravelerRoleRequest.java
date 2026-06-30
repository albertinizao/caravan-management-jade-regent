package com.gestioncaravana.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record UpdateCaravanTravelerRoleRequest(
    @NotEmpty List<String> activeRoleCodes,
    @NotBlank String activeRoleCode,
    Integer maxActiveRoleCount,
    UUID servedTravelerId) {}
