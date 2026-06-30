package com.gestioncaravana.adapter.in.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CaravanRequest(
    @NotBlank(message = "name is required") String name,
    String description,
    @Min(1) @Max(10) Integer offense,
    @Min(1) @Max(10) Integer defense,
    @Min(1) @Max(10) Integer mobility,
    @Min(1) @Max(10) Integer morale) {}

