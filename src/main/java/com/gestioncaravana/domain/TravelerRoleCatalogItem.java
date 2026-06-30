package com.gestioncaravana.domain;

public record TravelerRoleCatalogItem(
    String code,
    String name,
    String description,
    String requirements,
    boolean requiresTargetTraveler) {

  public TravelerRoleCatalogItem {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("code is required");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
  }
}
