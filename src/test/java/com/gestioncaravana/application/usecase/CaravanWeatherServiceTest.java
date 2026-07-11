package com.gestioncaravana.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWeatherProfileRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWeatherSnapshotRepositoryPort;
import com.gestioncaravana.domain.CaravanCampaign;
import com.gestioncaravana.domain.CaravanMainStats;
import com.gestioncaravana.domain.CaravanWeatherProfile;
import com.gestioncaravana.domain.CaravanWeatherSnapshot;
import com.gestioncaravana.domain.GolarionDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaravanWeatherServiceTest {

  private InMemoryCaravanRepository campaignRepository;
  private InMemoryWeatherProfileRepository weatherProfileRepository;
  private InMemoryWeatherSnapshotRepository weatherSnapshotRepository;
  private CaravanWeatherService service;
  private UUID caravanId;

  @BeforeEach
  void setUp() {
    campaignRepository = new InMemoryCaravanRepository();
    weatherProfileRepository = new InMemoryWeatherProfileRepository();
    weatherSnapshotRepository = new InMemoryWeatherSnapshotRepository();
    service = new CaravanWeatherService(
        campaignRepository,
        weatherProfileRepository,
        weatherSnapshotRepository,
        Clock.fixed(Instant.parse("2026-07-10T12:00:00Z"), ZoneOffset.UTC));
    caravanId = UUID.randomUUID();
    campaignRepository.save(CaravanCampaign.create(
        caravanId,
        "Weather Caravan",
        null,
        CaravanMainStats.initial(),
        Instant.parse("2026-01-01T00:00:00Z")));
  }

  @Test
  void generatesAndCachesWeatherSnapshots() {
    var date = new GolarionDate(4712, 1, 1);

    var first = service.getWeather(caravanId, date);
    var second = service.getWeather(caravanId, date);

    assertThat(first.midnightToDawn()).isNotNull();
    assertThat(second.midnightToDawn()).isEqualTo(first.midnightToDawn());
    assertThat(weatherSnapshotRepository.count()).isEqualTo(1);
  }

  @Test
  void updatesWeatherProfileAndPreservesEarlierSnapshots() {
    var earlierDate = new GolarionDate(4712, 1, 1);
    var effectiveFrom = new GolarionDate(4712, 1, 10);
    var laterDate = new GolarionDate(4712, 1, 12);
    service.getWeather(caravanId, earlierDate);
    service.getWeather(caravanId, laterDate);

    var updated = service.updateProfile(
        caravanId,
        new com.gestioncaravana.application.port.in.UpdateCaravanWeatherProfileUseCase.UpdateCaravanWeatherProfileCommand(
            com.gestioncaravana.domain.WeatherClimateBaseline.COLD,
            com.gestioncaravana.domain.WeatherElevation.PEAK,
            true,
            effectiveFrom));

    assertThat(updated.climateBaseline()).isEqualTo(com.gestioncaravana.domain.WeatherClimateBaseline.COLD);
    assertThat(weatherSnapshotRepository.findByCaravanIdAndDate(caravanId, earlierDate)).isPresent();
    assertThat(weatherSnapshotRepository.findByCaravanIdAndDate(caravanId, laterDate)).isEmpty();
    assertThat(weatherSnapshotRepository.count()).isEqualTo(1);
  }

  @Test
  void tropicalSeaLevelSummerMonthStaysInRealisticCelsiusBand() {
    weatherProfileRepository.save(new CaravanWeatherProfile(
        caravanId,
        com.gestioncaravana.domain.WeatherClimateBaseline.TROPICAL,
        com.gestioncaravana.domain.WeatherElevation.SEA_LEVEL,
        false,
        Instant.parse("2026-07-10T12:00:00Z")));

    for (int day = 1; day <= 31; day++) {
      var weather = service.getWeather(caravanId, new GolarionDate(4712, 7, day));
      assertThat(weather.midnightToDawn().temperatureC()).isBetween(10, 50);
      assertThat(weather.dawnToNoon().temperatureC()).isBetween(10, 50);
      assertThat(weather.noonToDusk().temperatureC()).isBetween(10, 50);
      assertThat(weather.duskToMidnight().temperatureC()).isBetween(10, 50);
    }
  }

  @Test
  void fogNeverAppearsWithWindStrongerThanLight() {
    for (int day = 1; day <= 31; day++) {
      var weather = service.getWeather(caravanId, new GolarionDate(4712, 1, day));
      assertFogHasLightWind(weather.midnightToDawn().precipitation(), weather.midnightToDawn().windStrength());
      assertFogHasLightWind(weather.dawnToNoon().precipitation(), weather.dawnToNoon().windStrength());
      assertFogHasLightWind(weather.noonToDusk().precipitation(), weather.noonToDusk().windStrength());
      assertFogHasLightWind(weather.duskToMidnight().precipitation(), weather.duskToMidnight().windStrength());
    }
  }

  @Test
  void consecutiveDaysKeepReasonableNoonTemperatureTransitions() {
    weatherProfileRepository.save(new CaravanWeatherProfile(
        caravanId,
        com.gestioncaravana.domain.WeatherClimateBaseline.TEMPERATE,
        com.gestioncaravana.domain.WeatherElevation.LOWLAND,
        false,
        Instant.parse("2026-07-10T12:00:00Z")));

    Integer previousNoon = null;
    for (int day = 1; day <= 31; day++) {
      var weather = service.getWeather(caravanId, new GolarionDate(4712, 3, day));
      var currentNoon = weather.noonToDusk().temperatureC();
      if (previousNoon != null) {
        assertThat(Math.abs(currentNoon - previousNoon)).isLessThanOrEqualTo(8);
      }
      previousNoon = currentNoon;
    }
  }

  private void assertFogHasLightWind(String precipitation, String windStrength) {
    if (precipitation != null && precipitation.contains("FOG")) {
      assertThat(windStrength).isEqualTo("LIGHT");
    }
  }

  private static final class InMemoryCaravanRepository implements CaravanCampaignRepositoryPort {
    private final Map<UUID, CaravanCampaign> caravans = new HashMap<>();

    @Override
    public CaravanCampaign save(CaravanCampaign caravanCampaign) {
      caravans.put(caravanCampaign.id(), caravanCampaign);
      return caravanCampaign;
    }

    @Override
    public void deleteById(UUID id) {
      caravans.remove(id);
    }

    @Override
    public java.util.List<CaravanCampaign> findAll() {
      return new java.util.ArrayList<>(caravans.values());
    }

    @Override
    public Optional<CaravanCampaign> findById(UUID id) {
      return Optional.ofNullable(caravans.get(id));
    }
  }

  private static final class InMemoryWeatherProfileRepository implements CaravanWeatherProfileRepositoryPort {
    private final Map<UUID, CaravanWeatherProfile> profiles = new HashMap<>();

    @Override
    public CaravanWeatherProfile save(CaravanWeatherProfile profile) {
      profiles.put(profile.caravanId(), profile);
      return profile;
    }

    @Override
    public Optional<CaravanWeatherProfile> findByCaravanId(UUID caravanId) {
      return Optional.ofNullable(profiles.get(caravanId));
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      profiles.remove(caravanId);
    }
  }

  private static final class InMemoryWeatherSnapshotRepository implements CaravanWeatherSnapshotRepositoryPort {
    private final Map<String, CaravanWeatherSnapshot> snapshots = new HashMap<>();

    @Override
    public CaravanWeatherSnapshot save(CaravanWeatherSnapshot snapshot) {
      snapshots.put(key(snapshot.caravanId(), snapshot.date()), snapshot);
      return snapshot;
    }

    @Override
    public Optional<CaravanWeatherSnapshot> findByCaravanIdAndDate(UUID caravanId, GolarionDate date) {
      return Optional.ofNullable(snapshots.get(key(caravanId, date)));
    }

    @Override
    public void deleteFromDate(UUID caravanId, GolarionDate fromDate) {
      snapshots.entrySet().removeIf(entry -> entry.getKey().startsWith(caravanId + ":")
          && toDate(entry.getKey()).compareTo(fromDate) >= 0);
    }

    @Override
    public void deleteByCaravanId(UUID caravanId) {
      snapshots.entrySet().removeIf(entry -> entry.getKey().startsWith(caravanId + ":"));
    }

    long count() {
      return snapshots.size();
    }

    private String key(UUID caravanId, GolarionDate date) {
      return caravanId + ":" + date.year() + ":" + date.month() + ":" + date.day();
    }

    private GolarionDate toDate(String key) {
      var parts = key.split(":");
      return new GolarionDate(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }
  }
}
