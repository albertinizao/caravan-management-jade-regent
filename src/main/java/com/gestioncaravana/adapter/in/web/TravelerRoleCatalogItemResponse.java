package com.gestioncaravana.adapter.in.web;

public record TravelerRoleCatalogItemResponse(
    String code,
    String name,
    String description,
    String requirements,
    boolean requiresTargetTraveler) {}
