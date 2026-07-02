package com.gestioncaravana.adapter.in.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

record AdvanceCaravanDayCycleRequest(
    String idempotencyKey,
    boolean fastingEnabled,
    @Valid List<CaravanDailyChoiceRequest> choices) {

  record CaravanDailyChoiceRequest(UUID travelerId, String mode) {}
}
