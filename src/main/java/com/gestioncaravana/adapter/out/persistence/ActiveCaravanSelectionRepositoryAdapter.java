package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.application.port.out.ActiveCaravanSelectionPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ActiveCaravanSelectionRepositoryAdapter implements ActiveCaravanSelectionPort {

  private final SpringDataActiveCaravanSelectionRepository repository;

  public ActiveCaravanSelectionRepositoryAdapter(SpringDataActiveCaravanSelectionRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<UUID> getActiveCaravanId() {
    return repository.findById(1L)
        .map(ActiveCaravanSelectionJpaEntity::getCaravanId)
        .map(UUID::fromString);
  }

  @Override
  public void setActiveCaravanId(UUID caravanId) {
    var entity = repository.findById(1L).orElseGet(ActiveCaravanSelectionJpaEntity::singleton);
    entity.setCaravanId(caravanId.toString());
    repository.save(entity);
  }

  @Override
  public void clear() {
    var selection = repository.findById(1L).orElseGet(ActiveCaravanSelectionJpaEntity::singleton);
    selection.setCaravanId(null);
    repository.save(selection);
  }
}
