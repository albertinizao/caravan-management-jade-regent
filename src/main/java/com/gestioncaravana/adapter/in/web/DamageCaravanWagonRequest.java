package com.gestioncaravana.adapter.in.web;

import jakarta.validation.constraints.Min;

public record DamageCaravanWagonRequest(@Min(1) int damageAmount, boolean ignoreHardness) {}
