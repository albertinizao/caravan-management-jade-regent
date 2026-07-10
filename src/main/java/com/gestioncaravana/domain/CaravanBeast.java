package com.gestioncaravana.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CaravanBeast(
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
    List<String> availableRoleCodes,
    String activeRoleCode,
    CaravanBeastAssignmentType assignmentType,
    UUID assignedWagonId,
    Instant createdAt,
    Instant updatedAt,
    BigDecimal occupiedSpace) {

  public CaravanBeast(
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
      Instant updatedAt) {
    this(
        id,
        caravanId,
        sourceType,
        catalogBeastCode,
        name,
        size,
        strength,
        speed,
        thermalAdaptation,
        basePrice,
        trainedPrice,
        fourLegged,
        specialNote,
        description,
        customNotes,
        consumption,
        List.of(),
        null,
        assignmentType,
        assignedWagonId,
        createdAt,
        updatedAt,
        BigDecimal.ONE);
  }

  public CaravanBeast(
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
      BigDecimal occupiedSpace) {
    this(
        id,
        caravanId,
        sourceType,
        catalogBeastCode,
        name,
        size,
        strength,
        speed,
        thermalAdaptation,
        basePrice,
        trainedPrice,
        fourLegged,
        specialNote,
        description,
        customNotes,
        consumption,
        List.of(),
        null,
        assignmentType,
        assignedWagonId,
        createdAt,
        updatedAt,
        occupiedSpace);
  }

  public CaravanBeast(
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
      List<String> availableRoleCodes,
      String activeRoleCode,
      int consumption,
      CaravanBeastAssignmentType assignmentType,
      UUID assignedWagonId,
      Instant createdAt,
      Instant updatedAt) {
    this(
        id,
        caravanId,
        sourceType,
        catalogBeastCode,
        name,
        size,
        strength,
        speed,
        thermalAdaptation,
        basePrice,
        trainedPrice,
        fourLegged,
        specialNote,
        description,
        customNotes,
        consumption,
        availableRoleCodes,
        activeRoleCode,
        assignmentType,
        assignedWagonId,
        createdAt,
        updatedAt,
        BigDecimal.ONE);
  }

  public CaravanBeast(
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
      List<String> availableRoleCodes,
      String activeRoleCode,
      int consumption,
      CaravanBeastAssignmentType assignmentType,
      UUID assignedWagonId,
      Instant createdAt,
      Instant updatedAt,
      BigDecimal occupiedSpace) {
    this(
        id,
        caravanId,
        sourceType,
        catalogBeastCode,
        name,
        size,
        strength,
        speed,
        thermalAdaptation,
        basePrice,
        trainedPrice,
        fourLegged,
        specialNote,
        description,
        customNotes,
        consumption,
        availableRoleCodes,
        activeRoleCode,
        assignmentType,
        assignedWagonId,
        createdAt,
        updatedAt,
        occupiedSpace);
  }

  public CaravanBeast {
    availableRoleCodes = normalizeRoleCodes(availableRoleCodes);
    activeRoleCode = normalize(activeRoleCode);
    if (id == null) {
      throw new IllegalArgumentException("id is required");
    }
    if (caravanId == null) {
      throw new IllegalArgumentException("caravanId is required");
    }
    if (sourceType == null) {
      throw new IllegalArgumentException("sourceType is required");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
    if (size == null || size.isBlank()) {
      throw new IllegalArgumentException("size is required");
    }
    if (strength < 0) {
      throw new IllegalArgumentException("strength must be greater than or equal to 0");
    }
    if (speed < 0) {
      throw new IllegalArgumentException("speed must be greater than or equal to 0");
    }
    if (specialNote == null || specialNote.isBlank()) {
      throw new IllegalArgumentException("specialNote is required");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("description is required");
    }
    if (consumption < 0) {
      throw new IllegalArgumentException("consumption must be greater than or equal to 0");
    }
    if (assignmentType == null) {
      throw new IllegalArgumentException("assignmentType is required");
    }
    if (assignmentType == CaravanBeastAssignmentType.NONE && assignedWagonId != null) {
      throw new IllegalArgumentException("assignedWagonId must be null when assignmentType is NONE");
    }
    if (assignmentType != CaravanBeastAssignmentType.NONE && assignedWagonId == null) {
      throw new IllegalArgumentException("assignedWagonId is required when assignmentType is not NONE");
    }
    if (createdAt == null || updatedAt == null) {
      throw new IllegalArgumentException("timestamps are required");
    }
    if (sourceType == CaravanBeastSourceType.CATALOG && (catalogBeastCode == null || catalogBeastCode.isBlank())) {
      throw new IllegalArgumentException("catalogBeastCode is required for catalog beasts");
    }
    if (sourceType == CaravanBeastSourceType.CUSTOM && catalogBeastCode != null && !catalogBeastCode.isBlank()) {
      throw new IllegalArgumentException("catalogBeastCode must be empty for custom beasts");
    }
    validateOccupiedSpace(occupiedSpace);
  }

  public static CaravanBeast createFromCatalog(
      UUID id,
      UUID caravanId,
      CaravanBeastCatalogItem catalogItem,
      Instant now) {
    return new CaravanBeast(
        id,
        caravanId,
        CaravanBeastSourceType.CATALOG,
        catalogItem.code(),
        catalogItem.name(),
        catalogItem.size(),
        catalogItem.strength(),
        catalogItem.speed(),
        catalogItem.thermalAdaptation(),
        catalogItem.basePrice(),
        catalogItem.trainedPrice(),
        catalogItem.fourLegged(),
        catalogItem.specialNote(),
        catalogItem.description(),
        catalogItem.notes(),
        1,
        List.of(),
        null,
        CaravanBeastAssignmentType.NONE,
        null,
        now,
        now,
        catalogItem.occupiedSpace() == null ? BigDecimal.ONE : catalogItem.occupiedSpace());
  }

  public static CaravanBeast createCustom(
      UUID id,
      UUID caravanId,
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
      Instant now) {
    return createCustom(
        id,
        caravanId,
        name,
        size,
        strength,
        speed,
        thermalAdaptation,
        basePrice,
        trainedPrice,
        fourLegged,
        specialNote,
        description,
        customNotes,
        null,
        BigDecimal.ONE,
        now);
  }

  public static CaravanBeast createCustom(
      UUID id,
      UUID caravanId,
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
      BigDecimal occupiedSpace,
      Instant now) {
    return createCustom(
        id,
        caravanId,
        name,
        size,
        strength,
        speed,
        thermalAdaptation,
        basePrice,
        trainedPrice,
        fourLegged,
        specialNote,
        description,
        customNotes,
        null,
        occupiedSpace,
        now);
  }

  public static CaravanBeast createCustom(
      UUID id,
      UUID caravanId,
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
      Integer consumption,
      Instant now) {
    return createCustom(
        id,
        caravanId,
        name,
        size,
        strength,
        speed,
        thermalAdaptation,
        basePrice,
        trainedPrice,
        fourLegged,
        specialNote,
        description,
        customNotes,
        consumption,
        BigDecimal.ONE,
        now);
  }

  public static CaravanBeast createCustom(
      UUID id,
      UUID caravanId,
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
      Integer consumption,
      BigDecimal occupiedSpace,
      Instant now) {
    return new CaravanBeast(
        id,
        caravanId,
        CaravanBeastSourceType.CUSTOM,
        null,
        name,
        size,
        strength,
        speed,
        thermalAdaptation,
        basePrice,
        trainedPrice,
        fourLegged,
        specialNote,
        description,
        normalize(customNotes),
        consumption == null ? 1 : consumption,
        List.of(),
        null,
        CaravanBeastAssignmentType.NONE,
        null,
        now,
        now,
        occupiedSpace == null ? BigDecimal.ONE : occupiedSpace);
  }

  public CaravanBeast updateCustomEconomy(Integer consumption, BigDecimal occupiedSpace, Instant now) {
    if (sourceType != CaravanBeastSourceType.CUSTOM) {
      throw new IllegalArgumentException("Only custom beasts can be updated");
    }
    var resolvedConsumption = consumption == null ? this.consumption : consumption;
    var resolvedOccupiedSpace = occupiedSpace == null ? this.occupiedSpace : occupiedSpace;
    return new CaravanBeast(
        id,
        caravanId,
        sourceType,
        catalogBeastCode,
        name,
        size,
        strength,
        speed,
        thermalAdaptation,
        basePrice,
        trainedPrice,
        fourLegged,
        specialNote,
        description,
        customNotes,
        resolvedConsumption,
        availableRoleCodes,
        activeRoleCode,
        assignmentType,
        assignedWagonId,
        createdAt,
        now,
        resolvedOccupiedSpace);
  }

  public CaravanBeast assignDraft(UUID wagonId, Instant now) {
    return new CaravanBeast(
        id,
        caravanId,
        sourceType,
        catalogBeastCode,
        name,
        size,
        strength,
        speed,
        thermalAdaptation,
        basePrice,
        trainedPrice,
        fourLegged,
        specialNote,
        description,
        customNotes,
        consumption,
        availableRoleCodes,
        activeRoleCode,
        CaravanBeastAssignmentType.DRAFT,
        wagonId,
        createdAt,
        now,
        occupiedSpace);
  }

  public CaravanBeast assignTraveler(UUID wagonId, Instant now) {
    return assignTraveler(wagonId, availableRoleCodes, activeRoleCode, now);
  }

  public CaravanBeast assignTraveler(
      UUID wagonId,
      List<String> availableRoleCodes,
      String activeRoleCode,
      Instant now) {
    return new CaravanBeast(
        id,
        caravanId,
        sourceType,
        catalogBeastCode,
        name,
        size,
        strength,
        speed,
        thermalAdaptation,
        basePrice,
        trainedPrice,
        fourLegged,
        specialNote,
        description,
        customNotes,
        consumption,
        availableRoleCodes,
        activeRoleCode,
        CaravanBeastAssignmentType.TRAVELER,
        wagonId,
        createdAt,
        now,
        occupiedSpace);
  }

  public CaravanBeast clearAssignment(Instant now) {
    return new CaravanBeast(
        id,
        caravanId,
        sourceType,
        catalogBeastCode,
        name,
        size,
        strength,
        speed,
        thermalAdaptation,
        basePrice,
        trainedPrice,
        fourLegged,
        specialNote,
        description,
        customNotes,
        consumption,
        availableRoleCodes,
        activeRoleCode,
        CaravanBeastAssignmentType.NONE,
        null,
        createdAt,
        now,
        occupiedSpace);
  }

  private static String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private static List<String> normalizeRoleCodes(List<String> roleCodes) {
    if (roleCodes == null) {
      return List.of();
    }
    return roleCodes.stream()
        .map(CaravanBeast::normalize)
        .filter(code -> code != null && !code.isBlank())
        .distinct()
        .toList();
  }

  private static void validateOccupiedSpace(BigDecimal occupiedSpace) {
    if (occupiedSpace == null) {
      throw new IllegalArgumentException("occupiedSpace is required");
    }
    if (occupiedSpace.signum() < 0) {
      throw new IllegalArgumentException("occupiedSpace must be greater than or equal to 0");
    }
    if (occupiedSpace.compareTo(BigDecimal.valueOf(4)) > 0) {
      throw new IllegalArgumentException("occupiedSpace must be less than or equal to 4");
    }
    if (occupiedSpace.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0) {
      throw new IllegalArgumentException("occupiedSpace must use 0.5 increments");
    }
  }
}
