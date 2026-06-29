package com.gestioncaravana.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record AddCaravanWagonImprovementRequest(@NotBlank String improvementTypeCode) {}
