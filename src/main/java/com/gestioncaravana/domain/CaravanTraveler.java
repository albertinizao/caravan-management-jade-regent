package com.gestioncaravana.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CaravanTraveler(
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
    Instant createdAt,
    Instant updatedAt) {

  public CaravanTraveler(
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
      TravelerContract contract,
      int consumption,
      Instant createdAt,
      Instant updatedAt) {
    this(
        id,
        caravanId,
        fullName,
        description,
        availableRoleCodes,
        activeRoleCodes,
        activeRoleCode,
        maxActiveRoleCount,
        roleSpecificData,
        wagonId,
        null,
        contract,
        consumption,
        createdAt,
        updatedAt);
  }

  public CaravanTraveler {
    if (id == null) {
      throw new IllegalArgumentException("id is required");
    }
    if (caravanId == null) {
      throw new IllegalArgumentException("caravanId is required");
    }
    if (fullName == null || fullName.isBlank()) {
      throw new IllegalArgumentException("fullName is required");
    }
    if (availableRoleCodes == null || availableRoleCodes.isEmpty()) {
      throw new IllegalArgumentException("availableRoleCodes is required");
    }
    if (activeRoleCodes == null || activeRoleCodes.isEmpty()) {
      throw new IllegalArgumentException("activeRoleCodes is required");
    }
    if (maxActiveRoleCount < 1) {
      throw new IllegalArgumentException("maxActiveRoleCount must be greater than or equal to 1");
    }
    if (activeRoleCodes.size() > maxActiveRoleCount) {
      throw new IllegalArgumentException("activeRoleCodes cannot exceed maxActiveRoleCount");
    }
    if (consumption < 0) {
      throw new IllegalArgumentException("consumption must be greater than or equal to 0");
    }
    if (createdAt == null || updatedAt == null) {
      throw new IllegalArgumentException("timestamps are required");
    }
    if (availableRoleCodes.stream().anyMatch(code -> TravelerRoleCatalog.findByCode(code).isEmpty())) {
      throw new IllegalArgumentException("availableRoleCodes must contain known traveler role codes");
    }
    if (!availableRoleCodes.contains(TravelerRoleCatalog.PASSENGER_CODE)) {
      throw new IllegalArgumentException("availableRoleCodes must contain the mandatory passenger role");
    }
    if (activeRoleCodes.stream().anyMatch(code -> !availableRoleCodes.contains(code))) {
      throw new IllegalArgumentException("activeRoleCodes must be one of availableRoleCodes");
    }
    if (activeRoleCodes.stream().anyMatch(code -> TravelerRoleCatalog.findByCode(code).isEmpty())) {
      throw new IllegalArgumentException("activeRoleCodes must contain known traveler role codes");
    }
    if (activeRoleCode == null || activeRoleCode.isBlank()) {
      throw new IllegalArgumentException("activeRoleCode is required");
    }
    if (!availableRoleCodes.contains(activeRoleCode)) {
      throw new IllegalArgumentException("activeRoleCode must be one of availableRoleCodes");
    }
    if (!activeRoleCodes.contains(activeRoleCode)) {
      throw new IllegalArgumentException("activeRoleCode must be one of activeRoleCodes");
    }
    if (activeRoleCodes.stream().anyMatch(TravelerRoleCatalog::requiresTargetTraveler)
        && (roleSpecificData == null || roleSpecificData.servedTravelerId() == null)) {
      throw new IllegalArgumentException("servedTravelerId is required for role " + activeRoleCode);
    }
    var carreteroActive = activeRoleCodes.contains(TravelerRoleCatalog.CARRETERO_CODE);
    if (carreteroActive && drivingWagonId == null) {
      throw new IllegalArgumentException("drivingWagonId is required for role carretero");
    }
    if (!carreteroActive && drivingWagonId != null) {
      throw new IllegalArgumentException("drivingWagonId can only be assigned when role carretero is active");
    }
  }

  public static CaravanTraveler create(
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
      Instant now) {
    var normalizedRoles = availableRoleCodes.stream().map(String::trim).filter(code -> !code.isBlank()).distinct().toList();
    if (normalizedRoles.isEmpty()) {
      throw new IllegalArgumentException("availableRoleCodes is required");
    }
    var availableWithPassenger = normalizedRoles.contains(TravelerRoleCatalog.PASSENGER_CODE)
        ? normalizedRoles
        : java.util.stream.Stream.concat(normalizedRoles.stream(), java.util.stream.Stream.of(TravelerRoleCatalog.PASSENGER_CODE))
            .distinct()
            .toList();
    var resolvedActiveRoles = (activeRoleCodes == null || activeRoleCodes.isEmpty())
        ? List.of(activeRoleCode == null || activeRoleCode.isBlank() ? normalizedRoles.getFirst() : activeRoleCode.trim())
        : activeRoleCodes.stream().map(String::trim).filter(code -> !code.isBlank()).distinct().toList();
    var resolvedActiveRole = activeRoleCode == null || activeRoleCode.isBlank()
        ? resolvedActiveRoles.getFirst()
        : activeRoleCode.trim();
    return new CaravanTraveler(
        id,
        caravanId,
        fullName.trim(),
        normalize(description),
        availableWithPassenger,
        resolvedActiveRoles,
        resolvedActiveRole,
        maxActiveRoleCount,
        roleSpecificData == null ? TravelerRoleData.empty() : roleSpecificData,
        wagonId,
        drivingWagonId,
        contract,
        consumption,
        now,
        now);
  }

  public static CaravanTraveler create(
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
      TravelerContract contract,
      int consumption,
      Instant now) {
    return create(
        id,
        caravanId,
        fullName,
        description,
        availableRoleCodes,
        activeRoleCodes,
        activeRoleCode,
        maxActiveRoleCount,
        roleSpecificData,
        wagonId,
        null,
        contract,
        consumption,
        now);
  }

  public CaravanTraveler assignWagon(UUID wagonId, Instant now) {
    return new CaravanTraveler(
        id,
        caravanId,
        fullName,
        description,
        availableRoleCodes,
        activeRoleCodes,
        activeRoleCode,
        maxActiveRoleCount,
        roleSpecificData,
        wagonId,
        drivingWagonId,
        contract,
        consumption,
        createdAt,
        now);
  }

  public CaravanTraveler assignDrivingWagon(UUID drivingWagonId, Instant now) {
    return new CaravanTraveler(
        id,
        caravanId,
        fullName,
        description,
        availableRoleCodes,
        activeRoleCodes,
        activeRoleCode,
        maxActiveRoleCount,
        roleSpecificData,
        wagonId,
        drivingWagonId,
        contract,
        consumption,
        createdAt,
        now);
  }

  public CaravanTraveler withRoleSpecificData(TravelerRoleData roleData, Instant now) {
    return new CaravanTraveler(
        id,
        caravanId,
        fullName,
        description,
        availableRoleCodes,
        activeRoleCodes,
        activeRoleCode,
        maxActiveRoleCount,
        roleData == null ? TravelerRoleData.empty() : roleData,
        wagonId,
        drivingWagonId,
        contract,
        consumption,
        createdAt,
        now);
  }

  public CaravanTraveler changeRoles(
      List<String> roleCodes,
      String roleCode,
      int newMaxActiveRoleCount,
      TravelerRoleData roleData,
      UUID drivingWagonId,
      Instant now) {
    var normalizedRoles = roleCodes == null ? List.<String>of() : roleCodes.stream().map(String::trim).filter(code -> !code.isBlank()).distinct().toList();
    var normalized = roleCode == null ? null : roleCode.trim();
    return new CaravanTraveler(
        id,
        caravanId,
        fullName,
        description,
        availableRoleCodes,
        normalizedRoles.isEmpty() ? List.of(normalized) : normalizedRoles,
        normalized,
        newMaxActiveRoleCount,
        roleData == null ? TravelerRoleData.empty() : roleData,
        wagonId,
        drivingWagonId,
        contract,
        consumption,
        createdAt,
        now);
  }

  public CaravanTraveler changeRoles(
      List<String> roleCodes,
      String roleCode,
      int newMaxActiveRoleCount,
      TravelerRoleData roleData,
      Instant now) {
    return changeRoles(roleCodes, roleCode, newMaxActiveRoleCount, roleData, drivingWagonId, now);
  }

  public CaravanTraveler updateDetails(
      String fullName,
      String description,
      List<String> availableRoleCodes,
      List<String> activeRoleCodes,
      String activeRoleCode,
      int newMaxActiveRoleCount,
      TravelerRoleData roleData,
      UUID wagonId,
      UUID drivingWagonId,
      TravelerContract contract,
      int consumption,
      Instant now) {
    var normalizedAvailableRoles = availableRoleCodes == null
        ? this.availableRoleCodes
        : availableRoleCodes.stream().map(String::trim).filter(code -> !code.isBlank()).distinct().toList();
    var availableWithPassenger = normalizedAvailableRoles.contains(TravelerRoleCatalog.PASSENGER_CODE)
        ? normalizedAvailableRoles
        : java.util.stream.Stream.concat(
            normalizedAvailableRoles.stream(),
            java.util.stream.Stream.of(TravelerRoleCatalog.PASSENGER_CODE))
            .distinct()
            .toList();
    var normalizedActiveRoles = activeRoleCodes == null
        ? this.activeRoleCodes
        : activeRoleCodes.stream().map(String::trim).filter(code -> !code.isBlank()).distinct().toList();
    var resolvedActiveRoles = normalizedActiveRoles.isEmpty()
        ? List.of(TravelerRoleCatalog.PASSENGER_CODE)
        : normalizedActiveRoles;
    var resolvedActiveRole = activeRoleCode == null || activeRoleCode.isBlank()
        ? resolvedActiveRoles.getFirst()
        : activeRoleCode.trim();
    return new CaravanTraveler(
        id,
        caravanId,
        fullName == null ? this.fullName : fullName.trim(),
        normalize(description),
        availableWithPassenger,
        resolvedActiveRoles,
        resolvedActiveRole,
        newMaxActiveRoleCount,
        roleData == null ? TravelerRoleData.empty() : roleData,
        wagonId,
        drivingWagonId,
        contract,
        consumption,
        createdAt,
        now);
  }

  public CaravanTraveler updateDetails(
      String fullName,
      String description,
      List<String> availableRoleCodes,
      List<String> activeRoleCodes,
      String activeRoleCode,
      int newMaxActiveRoleCount,
      TravelerRoleData roleData,
      UUID wagonId,
      TravelerContract contract,
      int consumption,
      Instant now) {
    return updateDetails(
        fullName,
        description,
        availableRoleCodes,
        activeRoleCodes,
        activeRoleCode,
        newMaxActiveRoleCount,
        roleData,
        wagonId,
        drivingWagonId,
        contract,
        consumption,
        now);
  }

  public boolean hasActiveRole(String roleCode) {
    return roleCode != null && activeRoleCodes.stream().anyMatch(roleCode::equals);
  }

  private static String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
