package com.gestioncaravana.application.model;

import java.util.List;

public record CargoCatalogItemView(
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
    List<String> allowedWagonCodes) {}
