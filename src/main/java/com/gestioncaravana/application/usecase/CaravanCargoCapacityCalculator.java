package com.gestioncaravana.application.usecase;

final class CaravanCargoCapacityCalculator {

  private CaravanCargoCapacityCalculator() {}

  static int calculate(int baseCargoCapacity, int cargoManagerCount, int organizationFeatCount) {
    if (baseCargoCapacity <= 0) {
      return 0;
    }
    var multiplierCount = Math.max(0, cargoManagerCount) + Math.max(0, organizationFeatCount);
    var currentCargoCapacity = baseCargoCapacity * Math.pow(1.1d, multiplierCount);
    return Math.max(0, (int) Math.round(currentCargoCapacity));
  }

  static int applyCargoManagerBonus(int currentCargoCapacity, int cargoManagerCount) {
    return calculate(currentCargoCapacity, cargoManagerCount, 0);
  }

  static int applyOrganizationFeatBonus(int currentCargoCapacity, int organizationFeatCount) {
    return calculate(currentCargoCapacity, 0, organizationFeatCount);
  }
}
