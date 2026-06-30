package com.gestioncaravana.adapter.in.web;

public record CaravanBeastCatalogItemResponse(
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
    String notes) {}
