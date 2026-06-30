package com.gestioncaravana.adapter.in.web;

public record WagonCatalogItemResponse(
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
