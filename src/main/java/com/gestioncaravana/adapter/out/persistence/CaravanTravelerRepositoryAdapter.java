package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.domain.CaravanTraveler;
import com.gestioncaravana.domain.TravelerContract;
import com.gestioncaravana.domain.TravelerRoleData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CaravanTravelerRepositoryAdapter implements CaravanTravelerRepositoryPort {

  private final SpringDataCaravanTravelerRepository repository;

  public CaravanTravelerRepositoryAdapter(SpringDataCaravanTravelerRepository repository) {
    this.repository = repository;
  }

  @Override
  public CaravanTraveler save(CaravanTraveler traveler) {
    return toDomain(repository.save(toEntity(traveler)));
  }

  @Override
  public List<CaravanTraveler> findAllByCaravanId(UUID caravanId) {
    return repository.findAllByCaravanId(caravanId.toString()).stream().map(this::toDomain).toList();
  }

  @Override
  public Optional<CaravanTraveler> findById(UUID caravanId, UUID travelerId) {
    return repository.findByCaravanIdAndId(caravanId.toString(), travelerId.toString()).map(this::toDomain);
  }

  @Override
  public long countByCaravanIdAndWagonId(UUID caravanId, UUID wagonId) {
    return repository.countByCaravanIdAndWagonId(caravanId.toString(), wagonId.toString());
  }

  @Override
  public void deleteByCaravanIdAndId(UUID caravanId, UUID travelerId) {
    repository.deleteByCaravanIdAndId(caravanId.toString(), travelerId.toString());
  }

  @Override
  public void deleteByCaravanId(UUID caravanId) {
    repository.deleteAll(repository.findAllByCaravanId(caravanId.toString()));
  }

  private CaravanTravelerJpaEntity toEntity(CaravanTraveler traveler) {
    var entity = new CaravanTravelerJpaEntity();
    entity.setId(traveler.id().toString());
    entity.setCaravanId(traveler.caravanId().toString());
    entity.setFullName(traveler.fullName());
    entity.setDescription(traveler.description());
    entity.setAvailableRoleCodesCsv(String.join(",", traveler.availableRoleCodes()));
    entity.setActiveRoleCodesCsv(String.join(",", traveler.activeRoleCodes()));
    entity.setActiveRoleCode(traveler.activeRoleCode());
    entity.setMaxActiveRoleCount(traveler.maxActiveRoleCount());
    entity.setServedTravelerId(traveler.roleSpecificData() == null || traveler.roleSpecificData().servedTravelerId() == null
        ? null
        : traveler.roleSpecificData().servedTravelerId().toString());
    entity.setWagonId(traveler.wagonId() == null ? null : traveler.wagonId().toString());
    entity.setSalary(traveler.contract() == null ? null : traveler.contract().salary());
    entity.setContractConditions(traveler.contract() == null ? null : traveler.contract().conditions());
    entity.setConsumption(traveler.consumption());
    entity.setGeneratingFood(traveler.roleSpecificData() != null && traveler.roleSpecificData().generatingFood());
    entity.setDaysServing(traveler.roleSpecificData() == null ? 0 : traveler.roleSpecificData().daysServing());
    entity.setCreatedAt(traveler.createdAt());
    entity.setUpdatedAt(traveler.updatedAt());
    return entity;
  }

  private CaravanTraveler toDomain(CaravanTravelerJpaEntity entity) {
    var availableRoleCodes = split(entity.getAvailableRoleCodesCsv());
    if (!availableRoleCodes.contains(com.gestioncaravana.domain.TravelerRoleCatalog.PASSENGER_CODE) && !availableRoleCodes.isEmpty()) {
      availableRoleCodes = List.copyOf(java.util.stream.Stream.concat(
          availableRoleCodes.stream(),
          java.util.stream.Stream.of(com.gestioncaravana.domain.TravelerRoleCatalog.PASSENGER_CODE)).distinct().toList());
    }
    var activeRoleCodes = split(entity.getActiveRoleCodesCsv() == null || entity.getActiveRoleCodesCsv().isBlank()
        ? entity.getActiveRoleCode()
        : entity.getActiveRoleCodesCsv());
    if (activeRoleCodes.isEmpty() && !availableRoleCodes.isEmpty()) {
      activeRoleCodes = List.of(availableRoleCodes.getFirst());
    }
    var activeRoleCode = entity.getActiveRoleCode();
    if (activeRoleCode == null || activeRoleCode.isBlank()) {
      activeRoleCode = activeRoleCodes.isEmpty() ? null : activeRoleCodes.getFirst();
    }
    return new CaravanTraveler(
        UUID.fromString(entity.getId()),
        UUID.fromString(entity.getCaravanId()),
        entity.getFullName(),
        entity.getDescription(),
        availableRoleCodes,
        activeRoleCodes,
        activeRoleCode,
        entity.getMaxActiveRoleCount() == null || entity.getMaxActiveRoleCount() < 1 ? 1 : entity.getMaxActiveRoleCount(),
        new TravelerRoleData(
            entity.getServedTravelerId() == null ? null : UUID.fromString(entity.getServedTravelerId()),
            Boolean.TRUE.equals(entity.getGeneratingFood()),
            entity.getDaysServing() == null ? 0 : entity.getDaysServing()),
        entity.getWagonId() == null ? null : UUID.fromString(entity.getWagonId()),
        entity.getSalary() == null && normalize(entity.getContractConditions()) == null
            ? null
            : new TravelerContract(entity.getSalary(), normalize(entity.getContractConditions()), entity.getCreatedAt(), null),
        entity.getConsumption(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  private List<String> split(String csv) {
    if (csv == null || csv.isBlank()) {
      return List.of();
    }
    return java.util.Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(code -> !code.isBlank())
        .toList();
  }

  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
