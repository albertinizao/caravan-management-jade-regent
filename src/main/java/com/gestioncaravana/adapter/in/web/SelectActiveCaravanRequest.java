package com.gestioncaravana.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SelectActiveCaravanRequest(@NotNull UUID caravanId) {}

