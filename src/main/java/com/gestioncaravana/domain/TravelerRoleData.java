package com.gestioncaravana.domain;

import java.util.UUID;

public record TravelerRoleData(UUID servedTravelerId, boolean generatingFood, int daysServing) {
  public TravelerRoleData {
    if (daysServing < 0) {
      throw new IllegalArgumentException("daysServing must be greater than or equal to 0");
    }
  }

  public TravelerRoleData(UUID servedTravelerId) {
    this(servedTravelerId, false, 0);
  }

  public TravelerRoleData(UUID servedTravelerId, boolean generatingFood) {
    this(servedTravelerId, generatingFood, 0);
  }

  public static TravelerRoleData empty() {
    return new TravelerRoleData(null, false, 0);
  }

  public TravelerRoleData withGeneratingFood(boolean generatingFood) {
    return new TravelerRoleData(servedTravelerId, generatingFood, daysServing);
  }

  public TravelerRoleData withDaysServing(int daysServing) {
    return new TravelerRoleData(servedTravelerId, generatingFood, daysServing);
  }

  public TravelerRoleData incrementDaysServing() {
    return new TravelerRoleData(servedTravelerId, generatingFood, daysServing + 1);
  }

  public TravelerRoleData resetDaysServing() {
    return new TravelerRoleData(servedTravelerId, generatingFood, 0);
  }
}
