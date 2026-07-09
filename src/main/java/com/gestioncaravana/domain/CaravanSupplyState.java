package com.gestioncaravana.domain;

import java.time.Instant;
import java.util.UUID;

public record CaravanSupplyState(
    UUID caravanId,
    int provisionReserve,
    int standardReserve,
    int perishableReserve,
    int daysPassed,
    Instant updatedAt,
    String sharedJobProductivityState) {

  public CaravanSupplyState {
    if (caravanId == null) {
      throw new IllegalArgumentException("caravanId is required");
    }
    if (provisionReserve < 0) {
      throw new IllegalArgumentException("provisionReserve must be greater than or equal to 0");
    }
    if (standardReserve < 0) {
      throw new IllegalArgumentException("standardReserve must be greater than or equal to 0");
    }
    if (perishableReserve < 0) {
      throw new IllegalArgumentException("perishableReserve must be greater than or equal to 0");
    }
    if (daysPassed < 0) {
      throw new IllegalArgumentException("daysPassed must be greater than or equal to 0");
    }
    if (updatedAt == null) {
      throw new IllegalArgumentException("updatedAt is required");
    }
    sharedJobProductivityState = normalize(sharedJobProductivityState);
  }

  public CaravanSupplyState(
      UUID caravanId,
      int provisionReserve,
      int standardReserve,
      int perishableReserve,
      int daysPassed,
      Instant updatedAt) {
    this(caravanId, provisionReserve, standardReserve, perishableReserve, daysPassed, updatedAt, null);
  }

  public static CaravanSupplyState initial(UUID caravanId, Instant now) {
    return new CaravanSupplyState(caravanId, 0, 0, 0, 0, now, null);
  }

  public CaravanSupplyState advance(int provisionDelta, int standardDelta, int perishableDelta, Instant now) {
    return new CaravanSupplyState(
        caravanId,
        Math.max(0, provisionReserve + provisionDelta),
        Math.max(0, standardReserve + standardDelta),
        Math.max(0, perishableReserve + perishableDelta),
        daysPassed + 1,
        now,
        sharedJobProductivityState);
  }

  public CaravanSupplyState withSharedJobProductivityState(String sharedJobProductivityState, Instant now) {
    return new CaravanSupplyState(
        caravanId,
        provisionReserve,
        standardReserve,
        perishableReserve,
        daysPassed,
        now,
        sharedJobProductivityState);
  }

  private static String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
