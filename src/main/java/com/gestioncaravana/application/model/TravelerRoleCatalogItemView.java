package com.gestioncaravana.application.model;

public record TravelerRoleCatalogItemView(
    String code,
    String name,
    String description,
    String requirements,
    boolean requiresTargetTraveler,
    String helperBenefitMode,
    Integer helperPeriodDays) {}
