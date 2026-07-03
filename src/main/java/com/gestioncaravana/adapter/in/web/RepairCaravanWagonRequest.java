package com.gestioncaravana.adapter.in.web;

import jakarta.validation.constraints.Min;

public record RepairCaravanWagonRequest(@Min(1) int repairAmount) {}
