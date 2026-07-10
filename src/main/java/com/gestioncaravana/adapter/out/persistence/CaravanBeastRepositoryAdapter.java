package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.application.port.out.CaravanBeastRepositoryPort;
import com.gestioncaravana.domain.CaravanBeast;
import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import com.gestioncaravana.domain.CaravanBeastSourceType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CaravanBeastRepositoryAdapter implements CaravanBeastRepositoryPort {

  private final SpringDataCaravanBeastRepository repository;

  public CaravanBeastRepositoryAdapter(SpringDataCaravanBeastRepository repository) {
    this.repository = repository;
  }

  @Override
  public CaravanBeast save(CaravanBeast beast) {
    return toDomain(repository.save(toEntity(beast)));
  }

  @Override
  public List<CaravanBeast> findAllByCaravanId(UUID caravanId) {
    return repository.findAllByCaravanId(caravanId.toString()).stream().map(this::toDomain).toList();
  }

  @Override
  public List<CaravanBeast> findAllByCaravanIdAndAssignmentType(UUID caravanId, CaravanBeastAssignmentType assignmentType) {
    return findAllByCaravanId(caravanId).stream()
        .filter(beast -> beast.assignmentType() == assignmentType)
        .toList();
  }

  @Override
  public List<CaravanBeast> findAllByCaravanIdAndWagonIdAndAssignmentType(
      UUID caravanId, UUID wagonId, CaravanBeastAssignmentType assignmentType) {
    return findAllByCaravanId(caravanId).stream()
        .filter(beast -> assignmentType == beast.assignmentType() && wagonId.equals(beast.assignedWagonId()))
        .toList();
  }

  @Override
  public Optional<CaravanBeast> findById(UUID caravanId, UUID beastId) {
    return repository.findByCaravanIdAndId(caravanId.toString(), beastId.toString()).map(this::toDomain);
  }

  @Override
  public void deleteByCaravanIdAndId(UUID caravanId, UUID beastId) {
    repository.deleteByCaravanIdAndId(caravanId.toString(), beastId.toString());
  }

  @Override
  public void deleteByCaravanId(UUID caravanId) {
    repository.deleteAll(repository.findAllByCaravanId(caravanId.toString()));
  }

  private CaravanBeastJpaEntity toEntity(CaravanBeast beast) {
    var entity = new CaravanBeastJpaEntity();
    entity.setId(beast.id().toString());
    entity.setCaravanId(beast.caravanId().toString());
    entity.setSourceType(beast.sourceType().name());
    entity.setCatalogBeastCode(beast.catalogBeastCode());
    entity.setName(beast.name());
    entity.setSize(beast.size());
    entity.setStrength(beast.strength());
    entity.setSpeed(beast.speed());
    entity.setThermalAdaptation(beast.thermalAdaptation());
    entity.setBasePrice(beast.basePrice());
    entity.setTrainedPrice(beast.trainedPrice());
    entity.setFourLegged(beast.fourLegged());
    entity.setSpecialNote(beast.specialNote());
    entity.setDescription(beast.description());
    entity.setCustomNotes(beast.customNotes());
    entity.setConsumption(beast.consumption());
    entity.setAvailableRoleCodesCsv(beast.availableRoleCodes() == null ? null : String.join(",", beast.availableRoleCodes()));
    entity.setActiveRoleCode(beast.activeRoleCode());
    entity.setAssignmentType(beast.assignmentType().name());
    entity.setAssignedWagonId(beast.assignedWagonId() == null ? null : beast.assignedWagonId().toString());
    entity.setCreatedAt(beast.createdAt());
    entity.setUpdatedAt(beast.updatedAt());
    entity.setOccupiedSpace(beast.occupiedSpace());
    return entity;
  }

  private CaravanBeast toDomain(CaravanBeastJpaEntity entity) {
    return new CaravanBeast(
        UUID.fromString(entity.getId()),
        UUID.fromString(entity.getCaravanId()),
        CaravanBeastSourceType.valueOf(entity.getSourceType()),
        entity.getCatalogBeastCode(),
        entity.getName(),
        entity.getSize(),
        entity.getStrength(),
        entity.getSpeed(),
        entity.getThermalAdaptation(),
        entity.getBasePrice(),
        entity.getTrainedPrice(),
        entity.isFourLegged(),
        entity.getSpecialNote(),
        entity.getDescription(),
        entity.getCustomNotes(),
        entity.getConsumption() == null ? 1 : entity.getConsumption(),
        split(entity.getAvailableRoleCodesCsv()),
        entity.getActiveRoleCode(),
        CaravanBeastAssignmentType.valueOf(entity.getAssignmentType()),
        entity.getAssignedWagonId() == null ? null : UUID.fromString(entity.getAssignedWagonId()),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getOccupiedSpace() == null ? BigDecimal.ONE : entity.getOccupiedSpace());
  }

  private java.util.List<String> split(String csv) {
    if (csv == null || csv.isBlank()) {
      return java.util.List.of();
    }
    return java.util.Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(code -> !code.isBlank())
        .distinct()
        .toList();
  }
}
