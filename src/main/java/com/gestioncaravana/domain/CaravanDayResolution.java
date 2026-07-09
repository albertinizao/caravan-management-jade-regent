package com.gestioncaravana.domain;

import java.time.Instant;
import java.util.UUID;

public record CaravanDayResolution(
    UUID id,
    UUID caravanId,
    String idempotencyKey,
    int resolvedDayIndex,
    Instant resolvedAt,
    int startingReserve,
    int endingReserve,
    int totalConsumption,
    int totalGeneration,
    int netDelta,
    int shortage,
    String cargoMovementSummary,
    String choicesSummary,
    String contributionsSummary,
    String warningsSummary) {

  public CaravanDayResolution {
    if (id == null) {
      throw new IllegalArgumentException("id is required");
    }
    if (caravanId == null) {
      throw new IllegalArgumentException("caravanId is required");
    }
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new IllegalArgumentException("idempotencyKey is required");
    }
    if (resolvedDayIndex < 1) {
      throw new IllegalArgumentException("resolvedDayIndex must be greater than or equal to 1");
    }
    if (resolvedAt == null) {
      throw new IllegalArgumentException("resolvedAt is required");
    }
    if (startingReserve < 0 || endingReserve < 0 || totalConsumption < 0 || totalGeneration < 0 || shortage < 0) {
      throw new IllegalArgumentException("numeric fields must be greater than or equal to 0");
    }
    if (cargoMovementSummary == null) {
      cargoMovementSummary = "";
    }
    if (choicesSummary == null) {
      choicesSummary = "";
    }
    if (contributionsSummary == null) {
      contributionsSummary = "";
    }
    if (warningsSummary == null) {
      warningsSummary = "";
    }
  }
}
