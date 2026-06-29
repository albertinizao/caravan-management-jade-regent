package com.gestioncaravana.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record CaravanRequest(
    @NotBlank(message = "name is required") String name,
    String description) {}

