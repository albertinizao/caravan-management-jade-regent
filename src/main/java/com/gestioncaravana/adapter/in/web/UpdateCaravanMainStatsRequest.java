package com.gestioncaravana.adapter.in.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCaravanMainStatsRequest(
    @NotNull @Min(0) @Max(10) Integer offense,
    @NotNull @Min(0) @Max(10) Integer defense,
    @NotNull @Min(0) @Max(10) Integer mobility,
    @NotNull @Min(0) @Max(10) Integer morale) {}
