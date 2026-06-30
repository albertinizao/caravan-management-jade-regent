package com.gestioncaravana.domain;

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
    String notes) {

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
  }
}
