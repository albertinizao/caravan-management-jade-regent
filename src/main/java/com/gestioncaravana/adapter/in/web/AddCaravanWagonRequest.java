package com.gestioncaravana.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record AddCaravanWagonRequest(@NotBlank String wagonTypeCode, String displayName, String specificCommodity) {}
