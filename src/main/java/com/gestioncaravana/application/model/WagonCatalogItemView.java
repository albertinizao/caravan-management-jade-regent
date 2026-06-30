package com.gestioncaravana.application.model;

public record WagonCatalogItemView(
    String code,
    String name,
    String category,
    int cost,
    int hitPoints,
    int hardness,
    String propulsion,
    int travelerCapacity,
    int cargoCapacity,
    String limitKind,
    Integer limitFixedMax,
    Integer limitRatioDenominator,
    String limit,
    int consumption,
    String specialBenefit,
    String description,
    String notes) {}
