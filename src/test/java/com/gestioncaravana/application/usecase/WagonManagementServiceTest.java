package com.gestioncaravana.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanMainStats;
import com.gestioncaravana.domain.CaravanCampaignStatus;
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

class WagonManagementServiceTest {

  private InMemoryCaravanRepository caravanRepository;
  private InMemoryWagonRepository wagonRepository;
  private WagonManagementService service;

  @BeforeEach
  void setUp() {
    caravanRepository = new InMemoryCaravanRepository();
    wagonRepository = new InMemoryWagonRepository();
    service = new WagonManagementService(
        caravanRepository,
        wagonRepository,
        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void listsTheCatalog() {
    assertThat(service.list()).extracting("code").contains("carro-cubierto", "carro-de-viajeros", "carro-vacio");
  }

  @Test
  void addsAndListsWagonsForTheCaravan() {
    var caravan = createCaravan();

    var created = service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-cubierto"));

    assertThat(created.name()).isEqualTo("Carro Cubierto");
    assertThat(service.list(caravan.id())).hasSize(1);
    assertThat(service.getById(caravan.id(), created.id()).wagonTypeCode()).isEqualTo("carro-cubierto");
  }

  @Test
  void rejectsUnknownWagonTypes() {
    var caravan = createCaravan();

    assertThatThrownBy(() -> service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("unknown")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Wagon type not found");
  }

  @Test
  void enforcesPerTypeLimits() {
    var caravan = createCaravan();
    service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-arcano"));

    assertThatThrownBy(() -> service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-arcano")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Wagon type limit reached");
  }

  @Test
  void enforcesCaravanMaximumWagonCount() {
    var caravan = createCaravan();
    for (int i = 0; i < 11; i++) {
      service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-cubierto"));
    }

    assertThatThrownBy(() -> service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-cubierto")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Caravan wagon limit reached");
  }

  @Test
  void deletesWagonsFromAcaravan() {
    var caravan = createCaravan();
    var created = service.execute(caravan.id(), new com.gestioncaravana.application.port.in.AddCaravanWagonUseCase.AddCaravanWagonCommand("carro-cubierto"));

    service.delete(caravan.id(), created.id());

    assertThat(service.list(caravan.id())).isEmpty();
  }

  private CaravanCampaign createCaravan() {
    var caravan = CaravanCampaign.create(UUID.randomUUID(), "Campaign", null, Instant.parse("2026-01-01T00:00:00Z"));
    caravanRepository.save(caravan);
    return caravan;
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

  private static final class InMemoryWagonRepository implements CaravanWagonRepositoryPort {
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
}
