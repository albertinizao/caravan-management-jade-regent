package com.gestioncaravana.application.model;

import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import com.gestioncaravana.domain.CaravanBeastSourceType;
import com.gestioncaravana.domain.CaravanCampaignStatus;
import com.gestioncaravana.domain.CaravanCargoSourceType;
import com.gestioncaravana.domain.CaravanFeatAcquisitionSourceType;
import com.gestioncaravana.domain.CaravanMainStats;
import com.gestioncaravana.domain.CaravanWeatherForecastState;
import com.gestioncaravana.domain.CaravanWeatherProfile;
import com.gestioncaravana.domain.CaravanWeatherSnapshot;
import com.gestioncaravana.domain.CustomCalendarEvent;
import com.gestioncaravana.domain.TravelerContract;
import com.gestioncaravana.domain.TravelerRoleData;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CaravanBackupView(
    int schemaVersion,
    boolean active,
    CaravanSnapshot caravan,
    SupplyStateSnapshot supplyState,
    List<WagonSnapshot> wagons,
    List<WagonImprovementSnapshot> wagonImprovements,
    List<TravelerSnapshot> travelers,
    List<CargoSnapshot> cargo,
    List<BeastSnapshot> beasts,
    List<FeatSnapshot> feats,
    List<DayResolutionSnapshot> dayResolutions,
    List<CustomCalendarEvent> calendarEvents,
    CaravanWeatherProfile weatherProfile,
    List<CaravanWeatherForecastState> weatherForecastStates,
    List<CaravanWeatherSnapshot> weatherSnapshots) {

  public static final int CURRENT_SCHEMA_VERSION = 4;

  public CaravanBackupView {
    if (schemaVersion != CURRENT_SCHEMA_VERSION && schemaVersion != 3 && schemaVersion != 2) {
      throw new IllegalArgumentException("Unsupported backup schema version: " + schemaVersion);
    }
    if (caravan == null) {
      throw new IllegalArgumentException("caravan is required");
    }
    if (supplyState == null) {
      throw new IllegalArgumentException("supplyState is required");
    }
    if (wagons == null) {
      throw new IllegalArgumentException("wagons is required");
    }
    if (wagonImprovements == null) {
      throw new IllegalArgumentException("wagonImprovements is required");
    }
    if (travelers == null) {
      throw new IllegalArgumentException("travelers is required");
    }
    if (cargo == null) {
      throw new IllegalArgumentException("cargo is required");
    }
    if (beasts == null) {
      throw new IllegalArgumentException("beasts is required");
    }
    if (feats == null) {
      throw new IllegalArgumentException("feats is required");
    }
    if (dayResolutions == null) {
      throw new IllegalArgumentException("dayResolutions is required");
    }
    if (calendarEvents == null) {
      calendarEvents = List.of();
    }
    wagons = List.copyOf(wagons);
    wagonImprovements = List.copyOf(wagonImprovements);
    travelers = List.copyOf(travelers);
    cargo = List.copyOf(cargo);
    beasts = List.copyOf(beasts);
    feats = List.copyOf(feats);
    dayResolutions = List.copyOf(dayResolutions);
    calendarEvents = List.copyOf(calendarEvents);
    weatherForecastStates = weatherForecastStates == null ? List.of() : List.copyOf(weatherForecastStates);
    weatherSnapshots = weatherSnapshots == null ? List.of() : List.copyOf(weatherSnapshots);
  }

  public record CaravanSnapshot(
      UUID id,
      String name,
      String description,
      int level,
      CaravanMainStats mainStats,
      int discontent,
      CaravanCampaignStatus status,
      Instant createdAt,
      Instant updatedAt) {}

  public record SupplyStateSnapshot(
      UUID caravanId,
      int provisionReserve,
      int standardReserve,
      int perishableReserve,
      int daysPassed,
      Instant updatedAt,
      String sharedJobProductivityState) {}

  public record WagonSnapshot(
      UUID id,
      UUID caravanId,
      String wagonTypeCode,
      String displayName,
      String specificCommodity,
      Integer currentHitPoints,
      Instant createdAt,
      Instant updatedAt) {}

  public record WagonImprovementSnapshot(
      UUID id,
      UUID caravanId,
      UUID wagonId,
      String improvementTypeCode,
      Instant createdAt,
      Instant updatedAt) {}

  public record TravelerSnapshot(
      UUID id,
      UUID caravanId,
      String fullName,
      String description,
      List<String> availableRoleCodes,
      List<String> activeRoleCodes,
      String activeRoleCode,
      int maxActiveRoleCount,
      TravelerRoleData roleSpecificData,
      UUID wagonId,
      UUID drivingWagonId,
      TravelerContract contract,
      int consumption,
      BigDecimal occupiedSpace,
      Instant createdAt,
      Instant updatedAt) {}

  public record CargoSnapshot(
      UUID id,
      UUID caravanId,
      CaravanCargoSourceType sourceType,
      String catalogCode,
      String displayName,
      String category,
      int quantity,
      int cargoUnits,
      BigDecimal currentProvisions,
      Boolean dayPassed,
      UUID wagonId,
      String origin,
      String specificCommodity,
      String deity,
      String notes,
      Instant createdAt,
      Instant updatedAt) {}

  public record BeastSnapshot(
      UUID id,
      UUID caravanId,
      CaravanBeastSourceType sourceType,
      String catalogBeastCode,
      String name,
      String size,
      int strength,
      int speed,
      Integer thermalAdaptation,
      Integer basePrice,
      Integer trainedPrice,
      boolean fourLegged,
      String specialNote,
      String description,
      String customNotes,
      int consumption,
      CaravanBeastAssignmentType assignmentType,
      UUID assignedWagonId,
      Instant createdAt,
      Instant updatedAt,
      BigDecimal occupiedSpace) {}

  public record FeatSnapshot(
      UUID id,
      UUID caravanId,
      String featTypeCode,
      CaravanFeatAcquisitionSourceType acquisitionSourceType,
      Integer acquisitionLevel,
      String acquisitionCause,
      int selectionIndex,
      boolean active,
      Boolean manualApplies,
      String manualAppliesReason,
      Instant createdAt,
      Instant updatedAt) {}

  public record DayResolutionSnapshot(
      UUID id,
      UUID caravanId,
      String idempotencyKey,
      int resolvedDayIndex,
      Instant resolvedAt,
      int startingReserve,
      int endingReserve,
      int totalConsumption,
      int totalGeneration,
      int netDelta,
      int shortage,
      String cargoMovementSummary,
      String choicesSummary,
      String contributionsSummary,
      String warningsSummary) {}
}
