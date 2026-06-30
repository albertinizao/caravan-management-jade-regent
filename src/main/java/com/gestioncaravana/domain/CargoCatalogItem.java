package com.gestioncaravana.domain;

import java.util.List;

public record CargoCatalogItem(
    String code,
    String name,
    String category,
    String priceExpression,
    Integer defaultQuantity,
    boolean quantityEditable,
    Integer defaultCargoUnits,
    boolean cargoUnitsEditable,
    String quantityLabel,
    String benefitText,
    String description,
    String notes,
    List<String> requiredMetadataKeys,
    List<String> allowedWagonCodes) {

  public CargoCatalogItem {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("code is required");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
    if (category == null || category.isBlank()) {
      throw new IllegalArgumentException("category is required");
    }
    if (priceExpression == null || priceExpression.isBlank()) {
      throw new IllegalArgumentException("priceExpression is required");
    }
    if (quantityLabel == null || quantityLabel.isBlank()) {
      throw new IllegalArgumentException("quantityLabel is required");
    }
    if (benefitText == null || benefitText.isBlank()) {
      throw new IllegalArgumentException("benefitText is required");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("description is required");
    }
    requiredMetadataKeys = requiredMetadataKeys == null ? List.of() : List.copyOf(requiredMetadataKeys);
    allowedWagonCodes = allowedWagonCodes == null ? List.of() : List.copyOf(allowedWagonCodes);
  }

  public int resolvedDefaultQuantity() {
    return defaultQuantity == null ? 1 : defaultQuantity;
  }

  public int resolvedDefaultCargoUnits() {
    return defaultCargoUnits == null ? 1 : defaultCargoUnits;
  }
}
