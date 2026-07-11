package com.gestioncaravana.application.model;

public record CaravanBackupImportResultView(
    CaravanCampaignView caravan,
    CaravanBackupImportSummaryView summary) {}
