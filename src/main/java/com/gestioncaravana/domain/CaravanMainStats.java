package com.gestioncaravana.domain;

public record CaravanMainStats(
    int offense,
    int defense,
    int mobility,
    int morale,
    int unassignedPoints) {

  public CaravanMainStats {
    validateBoundedStat("offense", offense);
    validateBoundedStat("defense", defense);
    validateBoundedStat("mobility", mobility);
    validateBoundedStat("morale", morale);
    if (unassignedPoints < 0) {
      throw new IllegalArgumentException("unassignedPoints must be greater than or equal to 0");
    }
  }

  public static CaravanMainStats initial() {
    return new CaravanMainStats(1, 1, 1, 1, 3);
  }

  private static void validateBoundedStat(String name, int value) {
    if (value < 0 || value > 10) {
      throw new IllegalArgumentException(name + " must be between 0 and 10");
    }
  }
}

