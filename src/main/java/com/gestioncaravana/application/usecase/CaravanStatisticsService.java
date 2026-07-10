package com.gestioncaravana.application.usecase;

import com.gestioncaravana.application.model.CaravanDerivedStatsView;
import com.gestioncaravana.application.model.CaravanMainStatsView;
import com.gestioncaravana.application.model.CaravanOtherStatsView;
import com.gestioncaravana.application.model.CaravanStatContributionView;
import com.gestioncaravana.application.model.CaravanStatisticsView;
import com.gestioncaravana.application.port.in.GetCaravanStatisticsUseCase;
import com.gestioncaravana.application.port.out.CaravanBeastRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCargoRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanFeatRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanTravelerRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonImprovementRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWagonRepositoryPort;
import com.gestioncaravana.domain.CaravanBeast;
import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanCargo;
import com.gestioncaravana.domain.CaravanTraveler;
import com.gestioncaravana.domain.CaravanWagon;
import com.gestioncaravana.domain.CaravanWagonImprovement;
import com.gestioncaravana.domain.WagonCatalog;
import com.gestioncaravana.domain.WagonImprovementCatalog;
import com.gestioncaravana.domain.WagonImprovementType;
import com.gestioncaravana.domain.WagonType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CaravanStatisticsService implements GetCaravanStatisticsUseCase {

  private final CaravanCampaignRepositoryPort caravanRepository;
  private final CaravanWagonRepositoryPort wagonRepository;
  private final CaravanWagonImprovementRepositoryPort improvementRepository;
  private final CaravanTravelerRepositoryPort travelerRepository;
  private final CaravanBeastRepositoryPort beastRepository;
  private final CaravanCargoRepositoryPort cargoRepository;
  private final CaravanFeatRepositoryPort featRepository;

  public CaravanStatisticsService(
      CaravanCampaignRepositoryPort caravanRepository,
      CaravanWagonRepositoryPort wagonRepository,
      CaravanWagonImprovementRepositoryPort improvementRepository,
      CaravanTravelerRepositoryPort travelerRepository,
      CaravanBeastRepositoryPort beastRepository,
      CaravanCargoRepositoryPort cargoRepository,
      CaravanFeatRepositoryPort featRepository) {
    this.caravanRepository = caravanRepository;
    this.wagonRepository = wagonRepository;
    this.improvementRepository = improvementRepository;
    this.travelerRepository = travelerRepository;
    this.beastRepository = beastRepository;
    this.cargoRepository = cargoRepository;
    this.featRepository = featRepository;
  }

  @Override
  public CaravanStatisticsView getById(UUID caravanId) {
    var caravan = requireCaravan(caravanId);
    var wagons = wagonRepository.findAllByCaravanId(caravanId);
    var travelers = travelerRepository.findAllByCaravanId(caravanId);
    var beasts = beastRepository.findAllByCaravanId(caravanId);
    var cargo = cargoRepository.findAllByCaravanId(caravanId);
    var feats = featRepository.findAllByCaravanId(caravanId);

    var moraleBonus = countActiveFeats(feats, "caravana-familiar") + countActiveFeats(feats, "lider-de-la-caravana");
    var mainStats = new CaravanMainStatsView(
        caravan.mainStats().offense(),
        caravan.mainStats().defense(),
        caravan.mainStats().mobility(),
        caravan.mainStats().morale() + moraleBonus,
        caravan.mainStats().unassignedPoints());

    var contributions = new ArrayList<CaravanStatContributionView>();

    var derivedStats = deriveStats(caravan, mainStats, wagons, travelers, beasts, cargo, feats, contributions);
    var otherStats = deriveOtherStats(caravan, wagons, travelers, beasts, cargo, feats, contributions);
    var warnings = new ArrayList<String>();
    if (caravan.discontent() >= mainStats.morale()) {
      warnings.add("El descontento ha alcanzado o superado la moral de la caravana.");
    }
    if (travelers.stream().noneMatch(traveler -> traveler.hasActiveRole("adivino"))) {
      warnings.add("La caravana no tiene adivino: aplica el penalizador base de seguridad y determinación.");
    }
    if (otherStats.cargoRemaining() < 0) {
      warnings.add("La caravana supera su capacidad de cargamento.");
    }
    if (otherStats.travelerCount() + otherStats.travelingBeastCount() > otherStats.travelerCapacity()) {
      warnings.add("La caravana supera su capacidad de viajeros.");
    }
    if (otherStats.wagonCount() > otherStats.maxWagons()) {
      var wagonOverflow = otherStats.wagonCount() - otherStats.maxWagons();
      warnings.add("La caravana supera su límite de carros: aplica -1 a cualquier tirada por cada carro adicional (" + wagonOverflow + " por encima).");
    }

    return new CaravanStatisticsView(
        caravan.id(),
        caravan.level(),
        mainStats,
        derivedStats,
        otherStats,
        caravan.discontent(),
        mainStats.morale(),
        contributions.stream().sorted(Comparator.comparing(CaravanStatContributionView::statCode)).toList(),
        List.copyOf(warnings),
        caravan.updatedAt());
  }

  private CaravanDerivedStatsView deriveStats(
      CaravanCampaign caravan,
      CaravanMainStatsView mainStats,
      List<CaravanWagon> wagons,
      List<CaravanTraveler> travelers,
      List<com.gestioncaravana.domain.CaravanBeast> beasts,
      List<CaravanCargo> cargo,
      List<com.gestioncaravana.domain.CaravanFeat> feats,
      List<CaravanStatContributionView> contributions) {
    var offense = mainStats.offense();
    var defense = mainStats.defense();
    var security = mainStats.mobility();
    var determination = mainStats.morale();

    contributions.add(contribution("offense", "BASE", caravan.id().toString(), "Caravana", "+" + offense, "BASE", "Ofensiva base"));
    contributions.add(contribution("defense", "BASE", caravan.id().toString(), "Caravana", "+" + defense, "BASE", "Defensa base"));
    contributions.add(contribution("security", "BASE", caravan.id().toString(), "Caravana", "+" + security, "BASE", "Movilidad base como seguridad"));
    contributions.add(contribution("determination", "BASE", caravan.id().toString(), "Caravana", "+" + determination, "BASE", "Moral base como determinación"));

    var adivinoCount = countTravelersWithRole(travelers, "adivino");
    if (adivinoCount == 0) {
      security -= 2;
      determination -= 2;
      contributions.add(contribution("security", "ROLE", "adivino", "Sin adivino", "-2", "ADD", "Penalizador por no tener adivino"));
      contributions.add(contribution("determination", "ROLE", "adivino", "Sin adivino", "-2", "ADD", "Penalizador por no tener adivino"));
    }

    var familyFeat = countActiveFeats(feats, "caravana-familiar");
    if (familyFeat > 0) {
      contributions.add(contribution("morale", "FEAT", "caravana-familiar", "Caravana Familiar", "+" + familyFeat, "ADD", "Bonificación manual activa de Caravana Familiar"));
    }

    var leaderFeat = countActiveFeats(feats, "lider-de-la-caravana");
    if (leaderFeat > 0) {
      security += leaderFeat;
      determination += leaderFeat;
      contributions.add(contribution("security", "FEAT", "lider-de-la-caravana", "Líder de la caravana", "+" + leaderFeat, "ADD", "Bonificación manual activa de Líder de la caravana"));
      contributions.add(contribution("determination", "FEAT", "lider-de-la-caravana", "Líder de la caravana", "+" + leaderFeat, "ADD", "Bonificación manual activa de Líder de la caravana"));
      contributions.add(contribution("morale", "FEAT", "lider-de-la-caravana", "Líder de la caravana", "+" + leaderFeat, "ADD", "Bonificación manual activa de Líder de la caravana"));
    }

    var guards = countTravelersWithRole(travelers, "guarda");
    if (guards > 0) {
      offense += guards;
      security += guards;
      contributions.add(contribution("offense", "ROLE", "guarda", "Guardas", "+" + guards, "ADD", "Cada guarda aporta +1 a Ofensiva"));
      contributions.add(contribution("security", "ROLE", "guarda", "Guardas", "+" + guards, "ADD", "Cada guarda aporta +1 a Seguridad"));
    }

    var guides = countTravelersWithRole(travelers, "guia");
    if (guides > 0) {
      security += guides;
      contributions.add(contribution("security", "ROLE", "guia", "Guías", "+" + guides, "ADD", "Cada guía aporta +1 a Seguridad"));
    }

    var heroes = Math.min(4, countTravelersWithRole(travelers, "heroe"));
    if (heroes > 0) {
      security += heroes;
      determination += heroes;
      contributions.add(contribution("security", "ROLE", "heroe", "Héroes", "+" + heroes, "ADD", "Cada héroe aporta +1 a Seguridad"));
      contributions.add(contribution("determination", "ROLE", "heroe", "Héroes", "+" + heroes, "ADD", "Cada héroe aporta +1 a Determinación"));
    }

    var comedians = countTravelersWithRole(travelers, "comediante");
    if (comedians > 0) {
      determination += comedians;
      contributions.add(contribution("determination", "ROLE", "comediante", "Comediantes", "+" + comedians, "ADD", "Cada comediante aporta +1 a Determinación"));
    }

    var royalWagons = wagons.stream().filter(wagon -> "carruaje-real".equals(wagon.wagonTypeCode())).toList();
    for (var royalWagon : royalWagons) {
      var passengers = travelers.stream()
          .filter(traveler -> royalWagon.id().equals(traveler.wagonId()))
          .filter(traveler -> traveler.hasActiveRole("pasajero"))
          .count();
      if (passengers > 0) {
        var bonus = (int) passengers * 2;
        determination += bonus;
        contributions.add(contribution("determination", "WAGON", royalWagon.id().toString(), "Carruaje Real", "+" + bonus, "ADD", "Cada pasajero asignado aporta +2 a Determinación"));
      }
    }

    for (var wagon : wagons) {
      var wagonType = requireWagonType(wagon.wagonTypeCode());
      var wagonName = wagon.displayNameOr(wagonType.name());
      if ("carro-de-viajeros".equals(wagon.wagonTypeCode())) {
        defense += 1;
        contributions.add(contribution("armorClass", "WAGON", wagon.id().toString(), wagonName, "+1", "ADD", "Cada Carro de Viajeros aporta +1 CA"));
      }
      if ("carro-de-prisioneros".equals(wagon.wagonTypeCode())) {
        security += 2;
        contributions.add(contribution("security", "WAGON", wagon.id().toString(), wagonName, "+2", "ADD", "Cada Carro de Prisioneros aporta +2 Seguridad"));
      }
    }

    return new CaravanDerivedStatsView(offense, 10 + defense, security, determination);
  }

  private CaravanOtherStatsView deriveOtherStats(
      CaravanCampaign caravan,
      List<CaravanWagon> wagons,
      List<CaravanTraveler> travelers,
      List<com.gestioncaravana.domain.CaravanBeast> beasts,
      List<CaravanCargo> cargo,
      List<com.gestioncaravana.domain.CaravanFeat> feats,
      List<CaravanStatContributionView> contributions) {
    var travelerCapacity = 0;
    var cargoCapacity = 0;
    var consumption = 0;

    for (var wagon : wagons) {
      var wagonType = requireWagonType(wagon.wagonTypeCode());
      var derived = deriveWagonStats(
          wagon.displayNameOr(wagonType.name()),
          wagonType,
          improvementRepository.findAllByCaravanIdAndWagonId(caravan.id(), wagon.id()),
          beasts,
          wagon.id(),
          contributions);
      travelerCapacity += derived.travelerCapacity();
      cargoCapacity += derived.cargoCapacity();
      consumption += derived.consumption();
      contributions.add(contribution("travelerCapacity", "WAGON", wagon.id().toString(), derived.name(), "+" + derived.travelerCapacity(), "ADD", "Capacidad de viajeros del carro"));
      contributions.add(contribution("cargoCapacity", "WAGON", wagon.id().toString(), derived.name(), "+" + derived.cargoCapacity(), "ADD", "Capacidad de cargamento del carro"));
      contributions.add(contribution("consumption", "WAGON", wagon.id().toString(), derived.name(), "+" + derived.consumption(), "ADD", "Consumo base del carro"));
    }

    var cargoLoad = cargo.stream().mapToInt(CaravanCargo::cargoUnits).sum();
    var cargoManagers = countTravelersWithRole(travelers, "encargado-de-suministros");
    var impeccableOrganization = countActiveFeats(feats, "organizacion-impecable");
    var cargoCapacityBeforeBonuses = cargoCapacity;
    cargoCapacity = CaravanCargoCapacityCalculator.calculate(cargoCapacity, cargoManagers, impeccableOrganization);
    var cargoCapacityBonus = cargoCapacity - cargoCapacityBeforeBonuses;
    if (cargoCapacityBonus > 0) {
      contributions.add(contribution("cargoCapacity", "BONUS", "cargo-capacity-bonuses", "Bonos de cargamento", "+" + cargoCapacityBonus, "ADD", "Organización Impecable y Encargados de suministros aumentan la capacidad de cargamento en un 10% por selección activa"));
    }

    var wagonConsumption = consumption;
    var travelerConsumption = 0;
    for (var traveler : travelers) {
      var effectiveConsumption = effectiveTravelerConsumption(traveler);
      travelerConsumption += effectiveConsumption;
      if (traveler.hasActiveRole("batidor")) {
        contributions.add(contribution(
            "consumption",
            "ROLE",
            traveler.id().toString(),
            traveler.fullName(),
            String.valueOf(effectiveConsumption),
            "ADD",
            "Los batidores no cuentan para el consumo"));
      } else {
        contributions.add(contribution("consumption", "TRAVELER", traveler.id().toString(), traveler.fullName(), "+" + effectiveConsumption, "ADD", "Consumo del viajero"));
      }
    }

    var totalConsumption = wagonConsumption + travelerConsumption;

    var efficientConsumption = countActiveFeats(feats, "consumo-eficiente");
    if (efficientConsumption > 0) {
      var reducedConsumption = Math.max(0, totalConsumption - (2 * efficientConsumption));
      var floor = Math.max(0, wagonConsumption);
      var adjustedConsumption = Math.max(floor, reducedConsumption);
      contributions.add(contribution("consumption", "FEAT", "consumo-eficiente", "Consumo Eficiente", String.valueOf(adjustedConsumption - totalConsumption), "ADD", "Reduce el consumo total en 2 por selección sin bajar del consumo de carros"));
      totalConsumption = adjustedConsumption;
    }

    var speed = deriveSpeed(wagons, beasts, contributions);
    var beastCount = beasts.size();
    var travelingBeastCount = (int) beasts.stream()
        .filter(beast -> beast.assignmentType() == CaravanBeastAssignmentType.TRAVELER)
        .count();
    var additionalWagons = countActiveFeats(feats, "carros-adicionales");
    var maxWagons = 10 + caravan.level() + (caravan.level() * additionalWagons);
    if (additionalWagons > 0) {
      contributions.add(contribution("wagonCapacity", "FEAT", "carros-adicionales", "Carros Adicionales", "+" + (caravan.level() * additionalWagons), "ADD", "Cada selección aumenta el límite de carros en el nivel de la caravana"));
    }

    var cargoRemaining = cargoCapacity - cargoLoad;
    contributions.add(contribution("cargoLoad", "CARGO", caravan.id().toString(), "Carga transportada", "+" + cargoLoad, "BASE", "Carga total actual"));
    contributions.add(contribution("wagonCount", "WAGON", caravan.id().toString(), "Caravana", "+" + wagons.size(), "BASE", "Número de carros"));
    contributions.add(contribution("travelerCount", "TRAVELER", caravan.id().toString(), "Caravana", "+" + travelers.size(), "BASE", "Número de viajeros"));
    contributions.add(contribution(
        "travelingBeastCount",
        "BEAST",
        caravan.id().toString(),
        "Caravana",
        "+" + travelingBeastCount,
        "BASE",
        "Número de bestias asignadas como viajeras"));
    contributions.add(contribution("beastCount", "BEAST", caravan.id().toString(), "Caravana", "+" + beastCount, "BASE", "Número de bestias"));

    return new CaravanOtherStatsView(
        speed,
        travelerCapacity,
        cargoCapacity,
        cargoLoad,
        cargoRemaining,
        totalConsumption,
        travelers.size(),
        travelingBeastCount,
        wagons.size(),
        beastCount,
        maxWagons);
  }

  private int deriveSpeed(
      List<CaravanWagon> wagons,
      List<com.gestioncaravana.domain.CaravanBeast> beasts,
      List<CaravanStatContributionView> contributions) {
    var draftSpeed = beasts.stream()
        .filter(beast -> beast.assignmentType() == CaravanBeastAssignmentType.DRAFT)
        .mapToInt(CaravanBeast::speed)
        .min()
        .orElse(0);
    var speed = mapSpeedToMilesPerDay(draftSpeed);
    if (draftSpeed > 0) {
      contributions.add(contribution("speed", "BEAST", "draft", "Bestias de tiro", "+" + speed, "BASE", "Velocidad base por la bestia más lenta"));
    }

    if (speed > 0 && !wagons.isEmpty()) {
      if (allWagonsHaveImprovement(wagons, "ruedas-mejoradas")) {
        speed += 8;
        contributions.add(contribution("speed", "IMPROVEMENT", "ruedas-mejoradas", "Ruedas Mejoradas", "+8", "ADD", "Todos los carros tienen ruedas mejoradas"));
      }
      if (allWagonsHaveImprovement(wagons, "tiro-de-cuatro-caballos")) {
        speed += 4;
        contributions.add(contribution("speed", "IMPROVEMENT", "tiro-de-cuatro-caballos", "Tiro De Cuatro Caballos", "+4", "ADD", "Todos los carros tienen tiro de cuatro caballos"));
      }
      if (allWagonsHaveImprovement(wagons, "tiro-de-seis-caballos")) {
        speed += 4;
        contributions.add(contribution("speed", "IMPROVEMENT", "tiro-de-seis-caballos", "Tiro De Seis Caballos", "+4", "ADD", "Todos los carros tienen tiro de seis caballos"));
      }
      if (allWagonsHaveImprovement(wagons, "tiro-de-ocho-caballos")) {
        speed += 4;
        contributions.add(contribution("speed", "IMPROVEMENT", "tiro-de-ocho-caballos", "Tiro De Ocho Caballos", "+4", "ADD", "Todos los carros tienen tiro de ocho caballos"));
      }
    }
    return Math.max(0, speed);
  }

  private WagonStats deriveWagonStats(
      String wagonName,
      WagonType wagonType,
      List<CaravanWagonImprovement> improvements,
      List<CaravanBeast> beasts,
      UUID wagonId,
      List<CaravanStatContributionView> contributions) {
    var currentTravelerCapacity = wagonType.travelerCapacity();
    var currentCargoCapacity = wagonType.cargoCapacity();
    var currentConsumption = wagonType.consumption();

    for (var improvement : improvements) {
      var type = requireImprovementType(improvement.improvementTypeCode());
      if (type.travelerCapacityOverride() != null) {
        currentTravelerCapacity = type.travelerCapacityOverride();
      } else if (type.travelerCapacityMultiplier() != null) {
        currentTravelerCapacity += Math.max(
            type.travelerCapacityMinimumIncrement() == null ? 0 : type.travelerCapacityMinimumIncrement(),
            roundStat(currentTravelerCapacity * (type.travelerCapacityMultiplier() - 1)));
      } else if (type.travelerCapacityBonus() != null) {
        currentTravelerCapacity += type.travelerCapacityBonus();
      }

      if (type.cargoCapacityOverride() != null) {
        currentCargoCapacity = type.cargoCapacityOverride();
      } else if (type.cargoCapacityMultiplier() != null) {
        currentCargoCapacity += Math.max(
            type.cargoCapacityMinimumIncrement() == null ? 0 : type.cargoCapacityMinimumIncrement(),
            roundStat(currentCargoCapacity * (type.cargoCapacityMultiplier() - 1)));
      } else if (type.cargoCapacityBonus() != null) {
        currentCargoCapacity += type.cargoCapacityBonus();
      }

      if (type.consumptionBonus() != null) {
        currentConsumption += type.consumptionBonus();
      }
    }

    var beastConsumption = beasts.stream()
        .filter(beast -> beast.assignmentType() == CaravanBeastAssignmentType.TRAVELER)
        .filter(beast -> beast.assignedWagonId() != null && beast.assignedWagonId().equals(wagonId))
        .mapToInt(CaravanBeast::consumption)
        .sum();
    if (beastConsumption > 0) {
      contributions.add(contribution("consumption", "BEAST", wagonId.toString(), wagonName, "+" + beastConsumption, "ADD", "Consumo de bestias viajeras"));
    }
    currentConsumption += beastConsumption;

    return new WagonStats(wagonName, Math.max(0, currentTravelerCapacity), Math.max(0, currentCargoCapacity), Math.max(0, currentConsumption));
  }

  private boolean allWagonsHaveImprovement(List<CaravanWagon> wagons, String code) {
    if (wagons.isEmpty()) {
      return false;
    }
    return wagons.stream().allMatch(wagon -> improvementRepository.findAllByCaravanIdAndWagonId(wagon.caravanId(), wagon.id()).stream()
        .anyMatch(improvement -> code.equals(improvement.improvementTypeCode())));
  }

  private int countTravelersWithRole(List<CaravanTraveler> travelers, String roleCode) {
    return (int) travelers.stream().filter(traveler -> traveler.hasActiveRole(roleCode)).count();
  }

  private int effectiveTravelerConsumption(CaravanTraveler traveler) {
    return traveler.hasActiveRole("batidor") ? 0 : traveler.consumption();
  }

  private int countActiveFeats(List<com.gestioncaravana.domain.CaravanFeat> feats, String featTypeCode) {
    return (int) feats.stream()
        .filter(feat -> feat.active() && featTypeCode.equals(feat.featTypeCode()))
        .count();
  }

  private int mapSpeedToMilesPerDay(int speed) {
    if (speed <= 20) {
      return speed == 0 ? 0 : 8;
    }
    if (speed <= 30) {
      return 16;
    }
    if (speed <= 40) {
      return 24;
    }
    if (speed <= 50) {
      return 32;
    }
    return 40;
  }

  private WagonType requireWagonType(String wagonTypeCode) {
    return WagonCatalog.findByCode(wagonTypeCode)
        .orElseThrow(() -> new IllegalStateException("Unknown wagon catalog entry: " + wagonTypeCode));
  }

  private WagonImprovementType requireImprovementType(String code) {
    return WagonImprovementCatalog.findByCode(code)
        .orElseThrow(() -> new IllegalStateException("Unknown improvement catalog entry: " + code));
  }

  private CaravanCampaign requireCaravan(UUID caravanId) {
    return caravanRepository.findById(caravanId)
        .orElseThrow(() -> new IllegalArgumentException("Caravan not found: " + caravanId));
  }

  private CaravanStatContributionView contribution(
      String statCode,
      String sourceType,
      String sourceId,
      String sourceName,
      String modifier,
      String operation,
      String reason) {
    return new CaravanStatContributionView(statCode, sourceType, sourceId, sourceName, modifier, operation, reason);
  }

  private int roundStat(double value) {
    return (int) Math.round(value);
  }

  private record WagonStats(String name, int travelerCapacity, int cargoCapacity, int consumption) {}
}
