package com.gestioncaravana.application.model;

public record CaravanBackupImportSummaryView(
    int caravans,
    int supplyStates,
    int wagons,
    int wagonImprovements,
    int travelers,
    int cargo,
    int beasts,
    int feats,
    int dayResolutions,
    int calendarEvents,
    int weatherProfiles,
    int weatherForecastStates,
    int weatherSnapshots) {}
