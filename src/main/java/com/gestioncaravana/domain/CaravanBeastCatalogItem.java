package com.gestioncaravana.domain;

import java.math.BigDecimal;

public record CaravanBeastCatalogItem(
    String code,
    String name,
    Integer basePrice,
    Integer trainedPrice,
    String size,
    int strength,
    int speed,
    Integer thermalAdaptation,
    boolean fourLegged,
    String specialNote,
    String description,
    String notes,
    BigDecimal occupiedSpace) {

  public CaravanBeastCatalogItem(
      String code,
      String name,
      Integer basePrice,
      Integer trainedPrice,
      String size,
      int strength,
      int speed,
      Integer thermalAdaptation,
      boolean fourLegged,
      String specialNote,
      String description,
      String notes) {
    this(
        code,
        name,
        basePrice,
        trainedPrice,
        size,
        strength,
        speed,
        thermalAdaptation,
        fourLegged,
        specialNote,
        description,
        notes,
        BigDecimal.ONE);
  }

  public CaravanBeastCatalogItem {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("code is required");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
    if (size == null || size.isBlank()) {
      throw new IllegalArgumentException("size is required");
    }
    if (strength < 0) {
      throw new IllegalArgumentException("strength must be greater than or equal to 0");
    }
    if (speed < 0) {
      throw new IllegalArgumentException("speed must be greater than or equal to 0");
    }
    if (specialNote == null || specialNote.isBlank()) {
      throw new IllegalArgumentException("specialNote is required");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("description is required");
    }
    validateOccupiedSpace(occupiedSpace);
  }

  private static void validateOccupiedSpace(BigDecimal occupiedSpace) {
    if (occupiedSpace == null) {
      throw new IllegalArgumentException("occupiedSpace is required");
    }
    if (occupiedSpace.signum() < 0) {
      throw new IllegalArgumentException("occupiedSpace must be greater than or equal to 0");
    }
    if (occupiedSpace.compareTo(BigDecimal.valueOf(4)) > 0) {
      throw new IllegalArgumentException("occupiedSpace must be less than or equal to 4");
    }
    if (occupiedSpace.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0) {
      throw new IllegalArgumentException("occupiedSpace must use 0.5 increments");
    }
  }
}
