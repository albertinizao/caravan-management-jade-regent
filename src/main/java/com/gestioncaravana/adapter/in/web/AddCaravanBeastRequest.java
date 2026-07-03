package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.domain.CaravanBeastSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AddCaravanBeastRequest(
    @NotNull CaravanBeastSourceType sourceType,
    String catalogBeastCode,
    @NotBlank String name,
    @NotBlank String size,
    Integer strength,
    Integer speed,
    Integer thermalAdaptation,
    Integer basePrice,
    Integer trainedPrice,
    Boolean fourLegged,
    @NotBlank String specialNote,
    @NotBlank String description,
    String customNotes,
    BigDecimal occupiedSpace) {}
