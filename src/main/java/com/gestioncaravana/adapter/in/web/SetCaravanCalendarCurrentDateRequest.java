package com.gestioncaravana.adapter.in.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetCaravanCalendarCurrentDateRequest(
    @NotNull @Min(4712) @Max(4722) Integer year,
    @NotNull @Min(1) @Max(12) Integer month,
    @NotNull @Min(1) @Max(31) Integer day) {}
