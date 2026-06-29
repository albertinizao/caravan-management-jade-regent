package com.gestioncaravana.application.model;

public record CaravanMainStatsView(
    int offense,
    int defense,
    int mobility,
    int morale,
    int unassignedPoints) {}

