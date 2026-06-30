package com.gestioncaravana.adapter.in.web;

import java.util.List;

public record CargoCatalogItemResponse(
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
