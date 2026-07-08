package com.gestioncaravana.domain;

public record CaravanMainStats(
    int offense,
    int defense,
    int mobility,
    int morale,
    int unassignedPoints) {

  private static final int INITIAL_MAIN_STAT_VALUE = 1;
  private static final int MAX_INITIAL_UNASSIGNED_POINTS = 3;
  private static final int MAX_STAT_VALUE = 10;

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
    return new CaravanMainStats(INITIAL_MAIN_STAT_VALUE, INITIAL_MAIN_STAT_VALUE, INITIAL_MAIN_STAT_VALUE, INITIAL_MAIN_STAT_VALUE, MAX_INITIAL_UNASSIGNED_POINTS);
  }

  public static CaravanMainStats withInitialAllocation(
      Integer offense,
      Integer defense,
      Integer mobility,
      Integer morale) {
    var resolvedOffense = offense == null ? INITIAL_MAIN_STAT_VALUE : offense;
    var resolvedDefense = defense == null ? INITIAL_MAIN_STAT_VALUE : defense;
    var resolvedMobility = mobility == null ? INITIAL_MAIN_STAT_VALUE : mobility;
    var resolvedMorale = morale == null ? INITIAL_MAIN_STAT_VALUE : morale;

    validateInitialAllocation("offense", resolvedOffense);
    validateInitialAllocation("defense", resolvedDefense);
    validateInitialAllocation("mobility", resolvedMobility);
    validateInitialAllocation("morale", resolvedMorale);

    var unassignedPoints = INITIAL_MAIN_STAT_VALUE * 4 + MAX_INITIAL_UNASSIGNED_POINTS
        - (resolvedOffense + resolvedDefense + resolvedMobility + resolvedMorale);
    if (unassignedPoints < 0 || unassignedPoints > MAX_INITIAL_UNASSIGNED_POINTS) {
      throw new IllegalArgumentException("initial allocation must spend at most 3 points");
    }

    return new CaravanMainStats(resolvedOffense, resolvedDefense, resolvedMobility, resolvedMorale, unassignedPoints);
  }

  public static CaravanMainStats withUpdatedAllocation(
      int offense,
      int defense,
      int mobility,
      int morale,
      int totalPoints) {
    if (totalPoints < 4) {
      throw new IllegalArgumentException("totalPoints must be greater than or equal to 4");
    }
    validateBoundedStat("offense", offense);
    validateBoundedStat("defense", defense);
    validateBoundedStat("mobility", mobility);
    validateBoundedStat("morale", morale);

    var unassignedPoints = totalPoints - (offense + defense + mobility + morale);
    if (unassignedPoints < 0) {
      throw new IllegalArgumentException("updated allocation must not spend more points than available");
    }

    return new CaravanMainStats(offense, defense, mobility, morale, unassignedPoints);
  }

  public CaravanMainStats adjustUnassignedPoints(int delta) {
    var adjustedUnassignedPoints = unassignedPoints + delta;
    if (adjustedUnassignedPoints < 0) {
      throw new IllegalArgumentException("unassignedPoints must be greater than or equal to 0");
    }
    return new CaravanMainStats(offense, defense, mobility, morale, adjustedUnassignedPoints);
  }

  private static void validateBoundedStat(String name, int value) {
    if (value < 0 || value > MAX_STAT_VALUE) {
      throw new IllegalArgumentException(name + " must be between 0 and 10");
    }
  }

  private static void validateInitialAllocation(String name, int value) {
    if (value < INITIAL_MAIN_STAT_VALUE || value > MAX_STAT_VALUE) {
      throw new IllegalArgumentException(name + " must be between 1 and 10");
    }
  }
}

