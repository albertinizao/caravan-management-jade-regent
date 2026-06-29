package com.gestioncaravana.domain;

import java.util.List;

public record WagonImprovementType(
    String code,
    String name,
    String category,
    String costExpression,
    Integer hitPointsBonus,
    Double hitPointsMultiplier,
    Integer hardnessBonus,
    Double hardnessMultiplier,
    String propulsionEffect,
    Integer travelerCapacityBonus,
    Double travelerCapacityMultiplier,
    Integer travelerCapacityMinimumIncrement,
    Integer travelerCapacityOverride,
    Integer cargoCapacityBonus,
    Double cargoCapacityMultiplier,
    Integer cargoCapacityMinimumIncrement,
    Integer cargoCapacityOverride,
    Integer consumptionBonus,
    int maxPerWagon,
    boolean repeatable,
    String requiredBasePropulsionFragment,
    List<String> prerequisites,
    List<String> incompatibilities,
    String specialBenefit,
    String description,
    String notes) {

  public WagonImprovementType {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("code is required");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
    if (category == null || category.isBlank()) {
      throw new IllegalArgumentException("category is required");
    }
    if (costExpression == null || costExpression.isBlank()) {
      throw new IllegalArgumentException("costExpression is required");
    }
    if (maxPerWagon < 1) {
      throw new IllegalArgumentException("maxPerWagon must be greater than or equal to 1");
    }
    prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
    incompatibilities = incompatibilities == null ? List.of() : List.copyOf(incompatibilities);
    if (specialBenefit == null || specialBenefit.isBlank()) {
      throw new IllegalArgumentException("specialBenefit is required");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("description is required");
    }
  }

  public boolean isRepeatable() {
    return repeatable;
  }
}
