package com.gestioncaravana.application.usecase;

import com.gestioncaravana.application.model.CaravanWeatherProfileView;
import com.gestioncaravana.application.model.WeatherPeriodView;
import com.gestioncaravana.application.model.WeatherSnapshotView;
import com.gestioncaravana.application.port.in.GetCaravanWeatherProfileUseCase;
import com.gestioncaravana.application.port.in.GetCaravanWeatherSnapshotUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanWeatherProfileUseCase;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWeatherProfileRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWeatherSnapshotRepositoryPort;
import com.gestioncaravana.domain.CaravanWeatherProfile;
import com.gestioncaravana.domain.CaravanWeatherSnapshot;
import com.gestioncaravana.domain.GolarionDate;
import com.gestioncaravana.domain.WeatherClimateBaseline;
import com.gestioncaravana.domain.WeatherElevation;
import com.gestioncaravana.domain.WeatherPeriod;
import com.gestioncaravana.domain.WeatherSeason;
import com.gestioncaravana.domain.WeatherSnapshot;
import java.time.Clock;
import java.util.SplittableRandom;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CaravanWeatherService
    implements GetCaravanWeatherProfileUseCase,
        UpdateCaravanWeatherProfileUseCase,
        GetCaravanWeatherSnapshotUseCase {

  private final CaravanCampaignRepositoryPort campaignRepository;
  private final CaravanWeatherProfileRepositoryPort profileRepository;
  private final CaravanWeatherSnapshotRepositoryPort snapshotRepository;
  private final Clock clock;

  public CaravanWeatherService(
      CaravanCampaignRepositoryPort campaignRepository,
      CaravanWeatherProfileRepositoryPort profileRepository,
      CaravanWeatherSnapshotRepositoryPort snapshotRepository,
      Clock clock) {
    this.campaignRepository = campaignRepository;
    this.profileRepository = profileRepository;
    this.snapshotRepository = snapshotRepository;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public CaravanWeatherProfileView getProfile(UUID caravanId) {
    requireCaravan(caravanId);
    return toView(profileRepository.findByCaravanId(caravanId)
        .orElseGet(() -> CaravanWeatherProfile.defaultProfile(caravanId, clock.instant())));
  }

  @Override
  public CaravanWeatherProfileView updateProfile(UUID caravanId, UpdateCaravanWeatherProfileCommand command) {
    requireCaravan(caravanId);
    if (command.climateBaseline() == null) {
      throw new IllegalArgumentException("climateBaseline is required");
    }
    if (command.elevation() == null) {
      throw new IllegalArgumentException("elevation is required");
    }
    if (command.effectiveFrom() == null) {
      throw new IllegalArgumentException("effectiveFrom is required");
    }
    var saved = profileRepository.save(new CaravanWeatherProfile(
        caravanId,
        command.climateBaseline(),
        command.elevation(),
        command.crownOfWorld(),
        clock.instant()));
    snapshotRepository.deleteFromDate(caravanId, command.effectiveFrom());
    return toView(saved);
  }

  @Override
  public WeatherSnapshotView getWeather(UUID caravanId, GolarionDate date) {
    requireCaravan(caravanId);
    return snapshotRepository.findByCaravanIdAndDate(caravanId, date)
        .map(CaravanWeatherSnapshot::weather)
        .map(CaravanWeatherService::toView)
        .orElseGet(() -> {
          var profile = profileRepository.findByCaravanId(caravanId)
              .orElseGet(() -> CaravanWeatherProfile.defaultProfile(caravanId, clock.instant()));
          var generated = generateWeather(caravanId, date, profile);
          snapshotRepository.save(generated);
          return toView(generated.weather());
        });
  }

  public void deleteByCaravanId(UUID caravanId) {
    profileRepository.deleteByCaravanId(caravanId);
    snapshotRepository.deleteByCaravanId(caravanId);
  }

  private CaravanWeatherSnapshot generateWeather(UUID caravanId, GolarionDate date, CaravanWeatherProfile profile) {
    var seed = hashSeed(caravanId, date, profile);
    var random = new SplittableRandom(seed);
    var season = WeatherSeason.fromMonth(date.month());
    var baseTemperature = baseTemperature(profile.climateBaseline(), profile.elevation(), season, profile.crownOfWorld());
    var daySwing = swingForSeason(season);

    var midnight = period(random, baseTemperature - daySwing, profile, season, date, 0, true);
    var dawn = period(random, baseTemperature - (daySwing / 3), profile, season, date, 1, false);
    var noon = period(random, baseTemperature + (daySwing / 2), profile, season, date, 2, false);
    var dusk = period(random, baseTemperature - (daySwing / 6), profile, season, date, 3, true);

    return new CaravanWeatherSnapshot(
        caravanId,
        date,
        new WeatherSnapshot(midnight, dawn, noon, dusk),
        clock.instant());
  }

  private WeatherPeriod period(
      SplittableRandom random,
      int temperature,
      CaravanWeatherProfile profile,
      WeatherSeason season,
      GolarionDate date,
      int phase,
      boolean night) {
    var precipitationRoll = random.nextInt(100);
    var precipitation = choosePrecipitation(precipitationRoll, profile, season, temperature);
    var wind = chooseWind(random.nextInt(100), precipitation);
    var adjustedTemperature = temperature + temperatureNoise(random, date, phase) + (night ? -1 : 0);
    return new WeatherPeriod(
        precipitation,
        wind,
        adjustedTemperature,
        celsiusToFahrenheit(adjustedTemperature));
  }

  private String choosePrecipitation(int roll, CaravanWeatherProfile profile, WeatherSeason season, int temperature) {
    var baseChance = switch (season) {
      case WINTER -> 35;
      case SPRING, FALL -> 25;
      case SUMMER -> 18;
    };
    baseChance += profile.climateBaseline() == WeatherClimateBaseline.COLD ? 10 : 0;
    baseChance += profile.climateBaseline() == WeatherClimateBaseline.TROPICAL ? -5 : 0;
    baseChance += profile.elevation() == WeatherElevation.HIGHLAND ? 8 : 0;
    baseChance += profile.elevation() == WeatherElevation.PEAK ? 15 : 0;
    baseChance += profile.crownOfWorld() ? 10 : 0;
    if (temperature <= 32) {
      baseChance += 10;
    }

    if (roll >= baseChance) {
      return "NONE";
    }

    var frozen = temperature <= 32;
    var typeRoll = roll % 100;
    if (frozen) {
      if (typeRoll < 10) {
        return "LIGHT_FOG";
      }
      if (typeRoll < 30) {
        return "HEAVY_FOG";
      }
      if (typeRoll < 70) {
        return "LIGHT_SNOW";
      }
      if (typeRoll < 90) {
        return "MEDIUM_SNOW";
      }
      return "HEAVY_SNOW";
    }

    if (typeRoll < 10) {
      return "LIGHT_FOG";
    }
    if (typeRoll < 20) {
      return "MEDIUM_FOG";
    }
    if (typeRoll < 35) {
      return "DRIZZLE";
    }
    if (typeRoll < 60) {
      return "LIGHT_RAIN";
    }
    if (typeRoll < 80) {
      return "RAIN";
    }
    if (typeRoll < 92) {
      return "HEAVY_RAIN";
    }
    return "THUNDERSTORM";
  }

  private String chooseWind(int roll, String precipitation) {
    if ("THUNDERSTORM".equals(precipitation)) {
      if (roll < 50) {
        return "STRONG";
      }
      if (roll < 90) {
        return "SEVERE";
      }
      return "WINDSTORM";
    }

    if (roll < 50) {
      return "LIGHT";
    }
    if (roll < 80) {
      return "MODERATE";
    }
    if (roll < 92) {
      return "STRONG";
    }
    if (roll < 97) {
      return "SEVERE";
    }
    return "WINDSTORM";
  }

  private int temperatureNoise(SplittableRandom random, GolarionDate date, int phase) {
    var combined = (date.year() * 31L) + (date.month() * 7L) + date.day() + phase;
    return (int) (random.split().nextInt(-3, 4) + (combined % 2 == 0 ? 0 : 1));
  }

  private int swingForSeason(WeatherSeason season) {
    return switch (season) {
      case WINTER -> 14;
      case SPRING -> 16;
      case SUMMER -> 18;
      case FALL -> 15;
    };
  }

  private int baseTemperature(WeatherClimateBaseline baseline, WeatherElevation elevation, WeatherSeason season, boolean crownOfWorld) {
    int base = switch (baseline) {
      case COLD -> switch (season) {
        case WINTER -> crownOfWorld ? -10 : 20;
        case SPRING, FALL -> crownOfWorld ? 10 : 30;
        case SUMMER -> crownOfWorld ? 15 : 40;
      };
      case TEMPERATE -> switch (season) {
        case WINTER -> 30;
        case SPRING, FALL -> 60;
        case SUMMER -> 80;
      };
      case TROPICAL -> switch (season) {
        case WINTER -> 50;
        case SPRING, FALL -> 75;
        case SUMMER -> 95;
      };
    };
    return base + elevationAdjustment(elevation);
  }

  private int elevationAdjustment(WeatherElevation elevation) {
    return switch (elevation) {
      case PEAK -> -25;
      case HIGHLAND -> -10;
      case SEA_LEVEL -> 10;
      case LOWLAND -> 0;
    };
  }

  private long hashSeed(UUID caravanId, GolarionDate date, CaravanWeatherProfile profile) {
    long seed = caravanId.getMostSignificantBits() ^ caravanId.getLeastSignificantBits();
    seed = 31 * seed + date.year();
    seed = 31 * seed + date.month();
    seed = 31 * seed + date.day();
    seed = 31 * seed + profile.climateBaseline().ordinal();
    seed = 31 * seed + profile.elevation().ordinal();
    seed = 31 * seed + (profile.crownOfWorld() ? 1 : 0);
    return seed;
  }

  private void requireCaravan(UUID caravanId) {
    campaignRepository.findById(caravanId)
        .orElseThrow(() -> new IllegalArgumentException("Caravan not found: " + caravanId));
  }

  private static WeatherSnapshotView toView(WeatherSnapshot weather) {
    return new WeatherSnapshotView(
        toView(weather.midnightToDawn()),
        toView(weather.dawnToNoon()),
        toView(weather.noonToDusk()),
        toView(weather.duskToMidnight()));
  }

  private static WeatherPeriodView toView(WeatherPeriod weather) {
    return new WeatherPeriodView(
        weather.precipitation(),
        weather.windStrength(),
        weather.temperatureC(),
        weather.temperatureF());
  }

  private static CaravanWeatherProfileView toView(CaravanWeatherProfile profile) {
    return new CaravanWeatherProfileView(
        profile.caravanId(),
        profile.climateBaseline(),
        profile.elevation(),
        profile.crownOfWorld(),
        profile.updatedAt());
  }

  private static int celsiusToFahrenheit(int celsius) {
    return Math.round((celsius * 9 / 5.0f) + 32);
  }
}
