package com.gestioncaravana.adapter.in.web;

public record CalendarEventResponse(
    Long id,
    String name,
    String scope,
    String description,
    String category,
    boolean secret) {}
