package com.gestioncaravana.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CaravanWeatherProfileRequest(
    @NotBlank String climateBaseline,
    @NotBlank String elevation,
    boolean crownOfWorld,
    @NotNull Integer effectiveFromYear,
    @NotNull Integer effectiveFromMonth,
    @NotNull Integer effectiveFromDay) {}
