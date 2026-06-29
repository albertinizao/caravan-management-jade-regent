package com.gestioncaravana.adapter.in.web;

public record CaravanMainStatsResponse(
    int offense,
    int defense,
    int mobility,
    int morale,
    int unassignedPoints) {}

