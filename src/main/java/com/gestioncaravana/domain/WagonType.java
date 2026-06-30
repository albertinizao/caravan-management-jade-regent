package com.gestioncaravana.domain;

public record WagonType(
    String code,
    String name,
    String category,
    int cost,
    int hitPoints,
    int hardness,
    String propulsion,
    int travelerCapacity,
    int cargoCapacity,
    WagonLimit limit,
    int consumption,
    String specialBenefit,
    String description,
    String notes) {

  public WagonType {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("code is required");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
    if (category == null || category.isBlank()) {
      throw new IllegalArgumentException("category is required");
    }
    if (cost < 0) {
      throw new IllegalArgumentException("cost must be greater than or equal to 0");
    }
    if (hitPoints < 0) {
      throw new IllegalArgumentException("hitPoints must be greater than or equal to 0");
    }
    if (hardness < 0) {
      throw new IllegalArgumentException("hardness must be greater than or equal to 0");
    }
    if (propulsion == null || propulsion.isBlank()) {
      throw new IllegalArgumentException("propulsion is required");
    }
    if (travelerCapacity < 0) {
      throw new IllegalArgumentException("travelerCapacity must be greater than or equal to 0");
    }
    if (cargoCapacity < 0) {
      throw new IllegalArgumentException("cargoCapacity must be greater than or equal to 0");
    }
    if (limit == null) {
      throw new IllegalArgumentException("limit is required");
    }
    if (consumption < 0) {
      throw new IllegalArgumentException("consumption must be greater than or equal to 0");
    }
    if (specialBenefit == null || specialBenefit.isBlank()) {
      throw new IllegalArgumentException("specialBenefit is required");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("description is required");
    }
  }

  public String limitLabel() {
    return limit.label();
  }
}
