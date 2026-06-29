package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanCampaignStatus;
import com.gestioncaravana.domain.CaravanMainStats;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CaravanCampaignRepositoryAdapter implements CaravanCampaignRepositoryPort {

  private final SpringDataCaravanCampaignRepository repository;

  public CaravanCampaignRepositoryAdapter(SpringDataCaravanCampaignRepository repository) {
    this.repository = repository;
  }

  @Override
  public CaravanCampaign save(CaravanCampaign caravanCampaign) {
    var saved = repository.save(toEntity(caravanCampaign));
    return toDomain(saved);
  }

  @Override
  public void deleteById(UUID id) {
    repository.deleteById(id.toString());
  }

  @Override
  public List<CaravanCampaign> findAll() {
    return repository.findAll().stream().map(this::toDomain).toList();
  }

  @Override
  public Optional<CaravanCampaign> findById(UUID id) {
    return repository.findById(id.toString()).map(this::toDomain);
  }

  private CaravanCampaignJpaEntity toEntity(CaravanCampaign caravanCampaign) {
    var entity = new CaravanCampaignJpaEntity();
    entity.setId(caravanCampaign.id().toString());
    entity.setName(caravanCampaign.name());
    entity.setDescription(caravanCampaign.description());
    entity.setLevel(caravanCampaign.level());
    entity.setMainStats(toEmbeddable(caravanCampaign.mainStats()));
    entity.setDiscontent(caravanCampaign.discontent());
    entity.setStatus(caravanCampaign.status().name());
    entity.setCreatedAt(caravanCampaign.createdAt());
    entity.setUpdatedAt(caravanCampaign.updatedAt());
    return entity;
  }

  private CaravanCampaign toDomain(CaravanCampaignJpaEntity entity) {
    return new CaravanCampaign(
        UUID.fromString(entity.getId()),
        entity.getName(),
        entity.getDescription(),
        entity.getLevel(),
        toDomain(entity.getMainStats()),
        entity.getDiscontent(),
        CaravanCampaignStatus.valueOf(entity.getStatus()),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  private CaravanMainStatsEmbeddable toEmbeddable(CaravanMainStats stats) {
    var embeddable = new CaravanMainStatsEmbeddable();
    embeddable.setOffense(stats.offense());
    embeddable.setDefense(stats.defense());
    embeddable.setMobility(stats.mobility());
    embeddable.setMorale(stats.morale());
    embeddable.setUnassignedPoints(stats.unassignedPoints());
    return embeddable;
  }

  private CaravanMainStats toDomain(CaravanMainStatsEmbeddable stats) {
    return new CaravanMainStats(
        stats.getOffense(),
        stats.getDefense(),
        stats.getMobility(),
        stats.getMorale(),
        stats.getUnassignedPoints());
  }
}
