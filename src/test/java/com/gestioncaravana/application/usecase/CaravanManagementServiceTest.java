package com.gestioncaravana.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestioncaravana.application.port.in.CreateCaravanUseCase.CreateCaravanCommand;
import com.gestioncaravana.application.port.out.ActiveCaravanSelectionPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanTraveler;
import com.gestioncaravana.domain.CaravanWagon;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaravanManagementServiceTest {

  private InMemoryCaravanRepository repository;
  private InMemoryActiveSelection activeSelection;
  private CaravanManagementService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryCaravanRepository();
    activeSelection = new InMemoryActiveSelection();
    service = new CaravanManagementService(
        repository,
        new InMemoryCaravanWagonRepository(),
        new InMemoryTravelerRepository(),
        activeSelection,
        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void createsAndListsCaravans() {
    var created = service.execute(new CreateCaravanCommand("Campaign", null));

    assertThat(created.name()).isEqualTo("Campaign");
    assertThat(created.level()).isEqualTo(1);
    assertThat(created.active()).isFalse();
    assertThat(service.list()).hasSize(1);
  }

  @Test
  void selectsAndReturnsTheActiveCaravan() {
    var created = service.execute(new CreateCaravanCommand("Campaign", null));

    var selected = service.select(created.id());

    assertThat(selected.active()).isTrue();
    assertThat(service.getActive()).isPresent();
    assertThat(service.getActive()).map(caravan -> caravan.id()).hasValue(created.id());
  }

  @Test
  void deletesCaravansAndClearsTheActiveSelectionWhenNeeded() {
    var created = service.execute(new CreateCaravanCommand("Campaign", null));
    service.select(created.id());

    service.delete(created.id());

    assertThat(service.list()).isEmpty();
    assertThat(service.getActive()).isEmpty();
  }

  @Test
  void throwsWhenSelectingUnknownCaravan() {
    assertThatThrownBy(() -> service.select(UUID.randomUUID()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Caravan not found");
  }

  private static final class InMemoryCaravanRepository implements CaravanCampaignRepositoryPort {
    private final List<CaravanCampaign> caravans = new ArrayList<>();

    @Override
    public CaravanCampaign save(CaravanCampaign caravanCampaign) {
      caravans.removeIf(existing -> existing.id().equals(caravanCampaign.id()));
      caravans.add(caravanCampaign);
      return caravanCampaign;
    }

    @Override
    public void deleteById(UUID id) {
      caravans.removeIf(existing -> existing.id().equals(id));
    }

    @Override
    public List<CaravanCampaign> findAll() {
      return List.copyOf(caravans);
    }

    @Override
    public Optional<CaravanCampaign> findById(UUID id) {
      return caravans.stream().filter(caravan -> caravan.id().equals(id)).findFirst();
    }
  }

  private static final class InMemoryCaravanWagonRepository implements CaravanWagonRepositoryPort {
    private final List<CaravanWagon> wagons = new ArrayList<>();

    @Override
    public CaravanWagon save(CaravanWagon wagon) {
      wagons.removeIf(existing -> existing.id().equals(wagon.id()));
      wagons.add(wagon);
      return wagon;
    }

    @Override
    public List<CaravanWagon> findAllByCaravanId(UUID caravanId) {
      return wagons.stream().filter(wagon -> wagon.caravanId().equals(caravanId)).toList();
    }

    @Override
    public Optional<CaravanWagon> findById(UUID caravanId, UUID wagonId) {
      return wagons.stream()
          .filter(wagon -> wagon.caravanId().equals(caravanId) && wagon.id().equals(wagonId))
          .findFirst();
    }

    @Override
    public void deleteById(UUID caravanId, UUID wagonId) {
      wagons.removeIf(wagon -> wagon.caravanId().equals(caravanId) && wagon.id().equals(wagonId));
    }

    @Override
    public long countByCaravanId(UUID caravanId) {
      return wagons.stream().filter(wagon -> wagon.caravanId().equals(caravanId)).count();
    }

    @Override
    public long countByCaravanIdAndWagonTypeCode(UUID caravanId, String wagonTypeCode) {
      return wagons.stream()
          .filter(wagon -> wagon.caravanId().equals(caravanId) && wagon.wagonTypeCode().equals(wagonTypeCode))
          .count();
    }
  }

  private static final class InMemoryTravelerRepository implements CaravanTravelerRepositoryPort {
    private final List<CaravanTraveler> travelers = new ArrayList<>();

    @Override
    public CaravanTraveler save(CaravanTraveler traveler) {
      travelers.removeIf(existing -> existing.id().equals(traveler.id()));
      travelers.add(traveler);
      return traveler;
    }

    @Override
    public List<CaravanTraveler> findAllByCaravanId(UUID caravanId) {
      return travelers.stream().filter(traveler -> traveler.caravanId().equals(caravanId)).toList();
    }

    @Override
    public Optional<CaravanTraveler> findById(UUID caravanId, UUID travelerId) {
      return travelers.stream()
          .filter(traveler -> traveler.caravanId().equals(caravanId) && traveler.id().equals(travelerId))
          .findFirst();
    }

    @Override
    public long countByCaravanIdAndWagonId(UUID caravanId, UUID wagonId) {
      return travelers.stream()
          .filter(traveler -> traveler.caravanId().equals(caravanId) && wagonId.equals(traveler.wagonId()))
          .count();
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      travelers.removeIf(traveler -> traveler.caravanId().equals(caravanId));
    }
  }

  private static final class InMemoryActiveSelection implements ActiveCaravanSelectionPort {
    private UUID activeId;

    @Override
    public Optional<UUID> getActiveCaravanId() {
      return Optional.ofNullable(activeId);
    }

    @Override
    public void setActiveCaravanId(UUID caravanId) {
      this.activeId = caravanId;
    }

    @Override
    public void clear() {
      this.activeId = null;
    }
  }
}
