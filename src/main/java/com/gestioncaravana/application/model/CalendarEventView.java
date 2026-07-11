package com.gestioncaravana.application.model;

public record CalendarEventView(
    Long id,
    String name,
    String scope,
    String description,
    String category,
    boolean secret) {}
