package com.gestioncaravana.application.usecase;

import com.gestioncaravana.application.model.CaravanWeatherProfileView;
import com.gestioncaravana.application.model.WeatherPeriodView;
import com.gestioncaravana.application.model.WeatherSnapshotView;
import com.gestioncaravana.application.port.in.GetCaravanWeatherProfileUseCase;
import com.gestioncaravana.application.port.in.GetCaravanWeatherSnapshotUseCase;
import com.gestioncaravana.application.port.in.UpdateCaravanWeatherProfileUseCase;
import com.gestioncaravana.application.port.out.CaravanCampaignRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWeatherForecastStateRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWeatherProfileRepositoryPort;
import com.gestioncaravana.application.port.out.CaravanWeatherSnapshotRepositoryPort;
import com.gestioncaravana.domain.CaravanWeatherForecastState;
import com.gestioncaravana.domain.CaravanWeatherProfile;
import com.gestioncaravana.domain.CaravanWeatherSnapshot;
import com.gestioncaravana.domain.GolarionCalendar;
import com.gestioncaravana.domain.GolarionDate;
import com.gestioncaravana.domain.WeatherClimateBaseline;
import com.gestioncaravana.domain.WeatherElevation;
import com.gestioncaravana.domain.WeatherPeriod;
import com.gestioncaravana.domain.WeatherSeason;
import com.gestioncaravana.domain.WeatherSnapshot;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

  private static final String NONE = "NONE";
  private static final String LIGHT = "LIGHT";
  private static final String MODERATE = "MODERATE";
  private static final String STRONG = "STRONG";
  private static final String SEVERE = "SEVERE";
  private static final String WINDSTORM = "WINDSTORM";
  private static final String NORMAL = "NORMAL";
  private static final String POLAR_TWILIGHT = "POLAR_TWILIGHT";
  private static final String POLAR_NIGHT = "POLAR_NIGHT";
  private static final String MIDNIGHT_SUN = "MIDNIGHT_SUN";
  private static final String POLAR_DAY = "POLAR_DAY";

  private final CaravanCampaignRepositoryPort campaignRepository;
  private final CaravanWeatherForecastStateRepositoryPort forecastStateRepository;
  private final CaravanWeatherProfileRepositoryPort profileRepository;
  private final CaravanWeatherSnapshotRepositoryPort snapshotRepository;
  private final Clock clock;

  public CaravanWeatherService(
      CaravanCampaignRepositoryPort campaignRepository,
      CaravanWeatherForecastStateRepositoryPort forecastStateRepository,
      CaravanWeatherProfileRepositoryPort profileRepository,
      CaravanWeatherSnapshotRepositoryPort snapshotRepository,
      Clock clock) {
    this.campaignRepository = campaignRepository;
    this.forecastStateRepository = forecastStateRepository;
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
    forecastStateRepository.deleteFromDate(caravanId, command.effectiveFrom());
    snapshotRepository.deleteFromDate(caravanId, command.effectiveFrom());
    return toView(saved);
  }

  @Override
  public WeatherSnapshotView getWeather(UUID caravanId, GolarionDate date) {
    requireCaravan(caravanId);
    var profile = profileRepository.findByCaravanId(caravanId)
        .orElseGet(() -> CaravanWeatherProfile.defaultProfile(caravanId, clock.instant()));
    var existing = snapshotRepository.findByCaravanIdAndDate(caravanId, date);
    if (existing.isPresent()) {
      return toView(existing.get().weather(), crownLightCondition(profile, date));
    }

    generateWeatherUpTo(caravanId, date, profile);
    return snapshotRepository.findByCaravanIdAndDate(caravanId, date)
        .map(CaravanWeatherSnapshot::weather)
        .map(weather -> toView(weather, crownLightCondition(profile, date)))
        .orElseThrow(() -> new IllegalStateException("Failed to generate weather snapshot for " + date));
  }

  public void deleteByCaravanId(UUID caravanId) {
    profileRepository.deleteByCaravanId(caravanId);
    forecastStateRepository.deleteByCaravanId(caravanId);
    snapshotRepository.deleteByCaravanId(caravanId);
  }

  private void generateWeatherUpTo(UUID caravanId, GolarionDate targetDate, CaravanWeatherProfile profile) {
    var previousState = findLatestState(caravanId, targetDate);
    var currentDate = previousState
        .map(state -> GolarionCalendar.addDays(state.date(), 1))
        .orElse(GolarionCalendar.MIN_SUPPORTED_DATE);

    while (currentDate.compareTo(targetDate) <= 0) {
      var generated = generateWeatherForDate(caravanId, currentDate, profile, previousState.orElse(null));
      snapshotRepository.save(generated.snapshot());
      forecastStateRepository.save(generated.forecastState());
      previousState = Optional.of(generated.forecastState());
      currentDate = GolarionCalendar.addDays(currentDate, 1);
    }
  }

  private Optional<CaravanWeatherForecastState> findLatestState(UUID caravanId, GolarionDate targetDate) {
    var current = targetDate;
    while (current.compareTo(GolarionCalendar.MIN_SUPPORTED_DATE) >= 0) {
      var state = forecastStateRepository.findByCaravanIdAndDate(caravanId, current);
      if (state.isPresent()) {
        return state;
      }
      if (current.equals(GolarionCalendar.MIN_SUPPORTED_DATE)) {
        break;
      }
      current = GolarionCalendar.addDays(current, -1);
    }
    return Optional.empty();
  }

  private GeneratedWeather generateWeatherForDate(
      UUID caravanId,
      GolarionDate date,
      CaravanWeatherProfile profile,
      CaravanWeatherForecastState previousState) {
    var season = WeatherSeason.fromMonth(date.month());
    var random = new SplittableRandom(hashSeed(caravanId, date, profile, previousState));
    var crownLightCondition = crownLightCondition(profile, date);

    var trend = resolveTemperatureTrend(profile, season, crownLightCondition, previousState, random);
    var precipitation = resolveDailyPrecipitation(profile, season, trend, previousState, random);
    var cloudCover = resolveCloudCover(precipitation, previousState, random);
    var windByPeriod = resolveWinds(precipitation, random);
    var severeEvent = resolveSevereEvent(profile, season, precipitation, windByPeriod, random);
    var adjustedTrend = applySevereEvent(trend, severeEvent, random);
    var periods = resolvePeriods(adjustedTrend, precipitation, cloudCover, windByPeriod, crownLightCondition, random);

    var snapshot = new CaravanWeatherSnapshot(
        caravanId,
        date,
        new WeatherSnapshot(periods.get(0), periods.get(1), periods.get(2), periods.get(3)),
        clock.instant());
    var state = new CaravanWeatherForecastState(
        caravanId,
        date,
        adjustedTrend.targetTemperatureF(),
        adjustedTrend.remainingTargetDays(),
        adjustedTrend.dayBaseTemperatureF(),
        adjustedTrend.nightBaseTemperatureF(),
        precipitation.carryOverPrecipitation(),
        precipitation.carryOverRemainingPeriods(),
        severeEvent,
        clock.instant());
    return new GeneratedWeather(snapshot, state);
  }

  private TemperatureTrendState resolveTemperatureTrend(
      CaravanWeatherProfile profile,
      WeatherSeason season,
      String crownLightCondition,
      CaravanWeatherForecastState previousState,
      SplittableRandom random) {
    var seasonalMeanF = baselineTemperatureF(profile.climateBaseline(), profile.elevation(), season);
    seasonalMeanF += crownOfWorldAdjustment(profile, season, random);

    int targetTemperatureF;
    int remainingDays;
    int previousDayBaseF;

    if (previousState == null) {
      previousDayBaseF = seasonalMeanF + random.nextInt(-4, 5);
      targetTemperatureF = seasonalMeanF + randomVariationF(profile.climateBaseline(), random);
      remainingDays = randomDurationDays(profile.climateBaseline(), random);
    } else {
      previousDayBaseF = previousState.dayBaseTemperatureF();
      if (previousState.remainingTargetDays() <= 0) {
        targetTemperatureF = seasonalMeanF + randomVariationF(profile.climateBaseline(), random);
        remainingDays = randomDurationDays(profile.climateBaseline(), random);
      } else {
        targetTemperatureF = previousState.targetTemperatureF();
        remainingDays = previousState.remainingTargetDays();
      }
    }

    var deltaToTarget = targetTemperatureF - previousDayBaseF;
    var divisor = Math.max(1, remainingDays);
    var step = deltaToTarget / divisor;
    if (deltaToTarget > 0) {
      step = Math.max(1, step);
    } else if (deltaToTarget < 0) {
      step = Math.min(-1, step);
    }
    var driftNoise = random.nextInt(-2, 3);
    var newDayBaseF = previousDayBaseF + step + driftNoise;
    var diurnalRangeF = diurnalRangeF(
        profile.climateBaseline(),
        profile.elevation(),
        season,
        crownLightCondition,
        random);
    var nightBaseF = newDayBaseF - diurnalRangeF;

    return new TemperatureTrendState(
        targetTemperatureF,
        Math.max(remainingDays - 1, 0),
        newDayBaseF,
        nightBaseF);
  }

  private DailyPrecipitation resolveDailyPrecipitation(
      CaravanWeatherProfile profile,
      WeatherSeason season,
      TemperatureTrendState trend,
      CaravanWeatherForecastState previousState,
      SplittableRandom random) {
    if (previousState != null
        && previousState.carryOverPrecipitation() != null
        && previousState.carryOverRemainingPeriods() > 0) {
      var continuingPeriods = Math.min(4, previousState.carryOverRemainingPeriods());
      return new DailyPrecipitation(
          previousState.carryOverPrecipitation(),
          0,
          continuingPeriods - 1,
          previousState.carryOverRemainingPeriods() - continuingPeriods,
          cloudCoverForPrecipitation(previousState.carryOverPrecipitation()));
    }

    var chance = precipitationChance(profile.climateBaseline(), profile.elevation(), season);
    if (profile.crownOfWorld()) {
      chance += 5;
    }
    if (random.nextInt(100) >= chance) {
      return DailyPrecipitation.none();
    }

    var averageTemperatureF = (trend.dayBaseTemperatureF() + trend.nightBaseTemperatureF()) / 2;
    var precipitation = choosePrecipitationType(profile, season, averageTemperatureF, random);
    var startPeriod = random.nextInt(4);
    var duration = 1 + random.nextInt(maxDurationPeriods(profile.climateBaseline(), precipitation));
    var endPeriod = Math.min(3, startPeriod + duration - 1);
    var carryOver = Math.max(0, (startPeriod + duration) - 4);
    return new DailyPrecipitation(
        precipitation,
        startPeriod,
        endPeriod,
        carryOver,
        cloudCoverForPrecipitation(precipitation));
  }

  private String resolveCloudCover(
      DailyPrecipitation precipitation,
      CaravanWeatherForecastState previousState,
      SplittableRandom random) {
    if (!NONE.equals(precipitation.precipitation())) {
      return precipitation.cloudCover();
    }
    if (previousState != null && previousState.carryOverPrecipitation() != null && random.nextInt(100) < 40) {
      return "PARTLY_CLOUDY";
    }
    var roll = random.nextInt(100);
    if (roll < 45) {
      return "CLEAR";
    }
    if (roll < 80) {
      return "PARTLY_CLOUDY";
    }
    return "OVERCAST";
  }

  private List<String> resolveWinds(DailyPrecipitation precipitation, SplittableRandom random) {
    var winds = new ArrayList<String>(List.of(LIGHT, LIGHT, LIGHT, LIGHT));
    for (int index = 0; index < 4; index++) {
      var hasPrecipitation = precipitation.affectsPeriod(index);
      if (!hasPrecipitation) {
        winds.set(index, rollFairWeatherWind(random));
        continue;
      }

      if (precipitation.isFog()) {
        winds.set(index, LIGHT);
        continue;
      }

      winds.set(index, rollStormWind(precipitation.precipitation(), random));
    }
    return winds;
  }

  private String resolveSevereEvent(
      CaravanWeatherProfile profile,
      WeatherSeason season,
      DailyPrecipitation precipitation,
      List<String> winds,
      SplittableRandom random) {
    var maxWind = winds.stream().mapToInt(this::windRank).max().orElse(0);
    if (precipitation.isThunderstorm() && maxWind >= windRank(STRONG) && random.nextInt(100) < 8) {
      return random.nextInt(100) < 35 ? "HAILSTORM" : "SEVERE_THUNDERSTORM";
    }
    if (precipitation.isSnowStorm() && maxWind >= windRank(SEVERE) && random.nextInt(100) < 10) {
      return random.nextInt(100) < 45 ? "BLIZZARD" : "THUNDERSNOW";
    }
    if (profile.crownOfWorld() && season != WeatherSeason.SUMMER && random.nextInt(100) < 6) {
      return "POLAR_SURGE";
    }
    return null;
  }

  private TemperatureTrendState applySevereEvent(
      TemperatureTrendState trend,
      String severeEvent,
      SplittableRandom random) {
    if (severeEvent == null) {
      return trend;
    }

    return switch (severeEvent) {
      case "POLAR_SURGE" -> new TemperatureTrendState(
          trend.targetTemperatureF() - 12,
          trend.remainingTargetDays(),
          trend.dayBaseTemperatureF() - random.nextInt(10, 19),
          trend.nightBaseTemperatureF() - random.nextInt(14, 24));
      case "BLIZZARD", "THUNDERSNOW" -> new TemperatureTrendState(
          trend.targetTemperatureF() - 6,
          trend.remainingTargetDays(),
          trend.dayBaseTemperatureF() - random.nextInt(4, 10),
          trend.nightBaseTemperatureF() - random.nextInt(6, 12));
      case "HAILSTORM", "SEVERE_THUNDERSTORM" -> new TemperatureTrendState(
          trend.targetTemperatureF(),
          trend.remainingTargetDays(),
          trend.dayBaseTemperatureF() - random.nextInt(2, 7),
          trend.nightBaseTemperatureF() - random.nextInt(1, 5));
      default -> trend;
    };
  }

  private List<WeatherPeriod> resolvePeriods(
      TemperatureTrendState trend,
      DailyPrecipitation precipitation,
      String cloudCover,
      List<String> winds,
      String crownLightCondition,
      SplittableRandom random) {
    if (POLAR_NIGHT.equals(crownLightCondition)
        || POLAR_DAY.equals(crownLightCondition)
        || MIDNIGHT_SUN.equals(crownLightCondition)) {
      return resolvePolarPeriods(trend, precipitation, winds, crownLightCondition, random);
    }
    var cloudAdjustment = cloudTemperatureAdjustment(cloudCover);
    var dawnF = trend.nightBaseTemperatureF() + cloudAdjustment.nightOffsetF() + random.nextInt(-1, 2);
    var morningF = ((trend.nightBaseTemperatureF() + trend.dayBaseTemperatureF()) / 2)
        + cloudAdjustment.transitionOffsetF() + random.nextInt(-1, 2);
    var afternoonF = trend.dayBaseTemperatureF() + cloudAdjustment.dayOffsetF() + random.nextInt(-1, 2);
    var nightF = (trend.nightBaseTemperatureF() + trend.dayBaseTemperatureF()) / 2
        - 2 + cloudAdjustment.nightOffsetF() + random.nextInt(-1, 2);

    if (morningF > afternoonF) {
      morningF = afternoonF - 1;
    }
    if (nightF > afternoonF) {
      nightF = afternoonF - 2;
    }

    return List.of(
        toPeriod(0, precipitation, winds.get(0), dawnF),
        toPeriod(1, precipitation, winds.get(1), morningF),
        toPeriod(2, precipitation, winds.get(2), afternoonF),
        toPeriod(3, precipitation, winds.get(3), nightF));
  }

  private List<WeatherPeriod> resolvePolarPeriods(
      TemperatureTrendState trend,
      DailyPrecipitation precipitation,
      List<String> winds,
      String crownLightCondition,
      SplittableRandom random) {
    var meanF = Math.round((trend.dayBaseTemperatureF() + trend.nightBaseTemperatureF()) / 2.0f);
    int midnightF;
    int morningF;
    int afternoonF;
    int nightF;

    switch (crownLightCondition) {
      case POLAR_NIGHT -> {
        midnightF = meanF + random.nextInt(-1, 1);
        morningF = meanF + random.nextInt(-1, 2);
        afternoonF = meanF + random.nextInt(0, 2);
        nightF = meanF + random.nextInt(-1, 1);
      }
      case POLAR_DAY -> {
        midnightF = meanF + random.nextInt(-1, 1);
        morningF = meanF + random.nextInt(0, 2);
        afternoonF = meanF + random.nextInt(1, 3);
        nightF = meanF + random.nextInt(0, 2);
      }
      case MIDNIGHT_SUN -> {
        midnightF = meanF + random.nextInt(-1, 2);
        morningF = meanF + random.nextInt(0, 3);
        afternoonF = meanF + random.nextInt(1, 4);
        nightF = meanF + random.nextInt(0, 2);
      }
      default -> throw new IllegalArgumentException("Unsupported polar condition: " + crownLightCondition);
    }

    return List.of(
        toPeriod(0, precipitation, winds.get(0), midnightF),
        toPeriod(1, precipitation, winds.get(1), morningF),
        toPeriod(2, precipitation, winds.get(2), afternoonF),
        toPeriod(3, precipitation, winds.get(3), nightF));
  }

  private WeatherPeriod toPeriod(int periodIndex, DailyPrecipitation precipitation, String wind, int temperatureF) {
    var precipitationLabel = precipitation.affectsPeriod(periodIndex) ? precipitation.precipitation() : NONE;
    var resolvedWind = precipitationLabel.contains("FOG") ? LIGHT : wind;
    return new WeatherPeriod(
        precipitationLabel,
        resolvedWind,
        fahrenheitToCelsius(temperatureF),
        temperatureF);
  }

  private int baselineTemperatureF(
      WeatherClimateBaseline baseline,
      WeatherElevation elevation,
      WeatherSeason season) {
    var climateBaseline = switch (baseline) {
      case COLD -> switch (season) {
        case WINTER -> 20;
        case SPRING, FALL -> 35;
        case SUMMER -> 50;
      };
      case TEMPERATE -> switch (season) {
        case WINTER -> 35;
        case SPRING, FALL -> 55;
        case SUMMER -> 75;
      };
      case TROPICAL -> switch (season) {
        case WINTER -> 70;
        case SPRING, FALL -> 82;
        case SUMMER -> 88;
      };
    };
    return climateBaseline + switch (elevation) {
      case SEA_LEVEL -> 3;
      case LOWLAND -> 0;
      case HIGHLAND -> -8;
      case PEAK -> -18;
    };
  }

  private int crownOfWorldAdjustment(
      CaravanWeatherProfile profile,
      WeatherSeason season,
      SplittableRandom random) {
    if (!profile.crownOfWorld()) {
      return 0;
    }
    return switch (season) {
      case WINTER -> -18 - random.nextInt(0, 5);
      case SPRING, FALL -> -12 - random.nextInt(0, 4);
      case SUMMER -> -8 - random.nextInt(0, 4);
    };
  }

  private int randomVariationF(WeatherClimateBaseline baseline, SplittableRandom random) {
    return switch (baseline) {
      case COLD -> random.nextInt(-16, 13);
      case TEMPERATE -> random.nextInt(-12, 13);
      case TROPICAL -> random.nextInt(-8, 10);
    };
  }

  private int randomDurationDays(WeatherClimateBaseline baseline, SplittableRandom random) {
    return switch (baseline) {
      case COLD -> random.nextInt(3, 7);
      case TEMPERATE -> random.nextInt(2, 6);
      case TROPICAL -> random.nextInt(2, 5);
    };
  }

  private int diurnalRangeF(
      WeatherClimateBaseline baseline,
      WeatherElevation elevation,
      WeatherSeason season,
      String crownLightCondition,
      SplittableRandom random) {
    var base = switch (baseline) {
      case COLD -> 10;
      case TEMPERATE -> 13;
      case TROPICAL -> 11;
    };
    base += switch (season) {
      case WINTER -> -1;
      case SPRING, FALL -> 0;
      case SUMMER -> 2;
    };
    base += switch (elevation) {
      case SEA_LEVEL -> 1;
      case LOWLAND -> 0;
      case HIGHLAND -> 1;
      case PEAK -> 2;
    };
    if (crownLightCondition != null) {
      base = switch (crownLightCondition) {
        case POLAR_NIGHT, POLAR_DAY -> Math.max(3, base / 4);
        case POLAR_TWILIGHT -> Math.max(5, base / 2);
        case MIDNIGHT_SUN -> Math.max(4, (base / 2) + 1);
        default -> base;
      };
    }
    return base + random.nextInt(-2, 3);
  }

  private int precipitationChance(
      WeatherClimateBaseline baseline,
      WeatherElevation elevation,
      WeatherSeason season) {
    var seasonal = switch (season) {
      case WINTER -> 28;
      case SPRING -> 32;
      case SUMMER -> 24;
      case FALL -> 30;
    };
    seasonal += switch (baseline) {
      case COLD -> -4;
      case TEMPERATE -> 0;
      case TROPICAL -> 18;
    };
    seasonal += switch (elevation) {
      case SEA_LEVEL -> 8;
      case LOWLAND -> 3;
      case HIGHLAND -> -5;
      case PEAK -> -8;
    };
    return seasonal;
  }

  private String choosePrecipitationType(
      CaravanWeatherProfile profile,
      WeatherSeason season,
      int averageTemperatureF,
      SplittableRandom random) {
    var roll = random.nextInt(100);
    var freezingBoundary = 32;
    if (averageTemperatureF <= freezingBoundary - 3) {
      if (roll < 20) {
        return "LIGHT_FOG";
      }
      if (roll < 60) {
        return "LIGHT_SNOW";
      }
      if (roll < 88) {
        return "MEDIUM_SNOW";
      }
      return "HEAVY_SNOW";
    }

    if (averageTemperatureF <= freezingBoundary + 2) {
      if (roll < 12) {
        return "LIGHT_FOG";
      }
      if (roll < 35) {
        return "SLEET";
      }
      if (roll < 60) {
        return "LIGHT_SNOW";
      }
      if (roll < 82) {
        return "LIGHT_RAIN";
      }
      return "RAIN";
    }

    if (roll < 12) {
      return season == WeatherSeason.SUMMER && profile.climateBaseline() != WeatherClimateBaseline.COLD
          ? "MORNING_MIST"
          : "LIGHT_FOG";
    }
    if (roll < 28) {
      return "DRIZZLE";
    }
    if (roll < 58) {
      return "LIGHT_RAIN";
    }
    if (roll < 82) {
      return "RAIN";
    }
    if (roll < 95) {
      return "HEAVY_RAIN";
    }
    return "THUNDERSTORM";
  }

  private int maxDurationPeriods(WeatherClimateBaseline baseline, String precipitation) {
    var base = switch (baseline) {
      case COLD -> 5;
      case TEMPERATE -> 4;
      case TROPICAL -> 6;
    };
    if (precipitation.contains("THUNDER")) {
      return Math.max(2, base - 1);
    }
    if (precipitation.contains("FOG") || precipitation.contains("MIST")) {
      return Math.max(2, base - 2);
    }
    return base;
  }

  private String cloudCoverForPrecipitation(String precipitation) {
    if (precipitation == null || NONE.equals(precipitation)) {
      return "CLEAR";
    }
    if (precipitation.contains("THUNDER") || precipitation.contains("HEAVY")) {
      return "STORM";
    }
    if (precipitation.contains("FOG") || precipitation.contains("MIST")) {
      return "OVERCAST";
    }
    return "OVERCAST";
  }

  private String rollFairWeatherWind(SplittableRandom random) {
    var roll = random.nextInt(100);
    if (roll < 60) {
      return LIGHT;
    }
    if (roll < 90) {
      return MODERATE;
    }
    if (roll < 98) {
      return STRONG;
    }
    return SEVERE;
  }

  private String rollStormWind(String precipitation, SplittableRandom random) {
    if (precipitation.contains("THUNDER")) {
      var roll = random.nextInt(100);
      if (roll < 45) {
        return STRONG;
      }
      if (roll < 85) {
        return SEVERE;
      }
      return WINDSTORM;
    }
    if (precipitation.contains("HEAVY")) {
      return random.nextInt(100) < 60 ? STRONG : SEVERE;
    }
    if (precipitation.contains("SNOW") || precipitation.contains("SLEET")) {
      return random.nextInt(100) < 70 ? MODERATE : STRONG;
    }
    return random.nextInt(100) < 75 ? LIGHT : MODERATE;
  }

  private int windRank(String wind) {
    return switch (wind) {
      case LIGHT -> 1;
      case MODERATE -> 2;
      case STRONG -> 3;
      case SEVERE -> 4;
      case WINDSTORM -> 5;
      default -> 0;
    };
  }

  private CloudTemperatureAdjustment cloudTemperatureAdjustment(String cloudCover) {
    return switch (cloudCover) {
      case "CLEAR" -> new CloudTemperatureAdjustment(0, 0, 0);
      case "PARTLY_CLOUDY" -> new CloudTemperatureAdjustment(-1, 1, 0);
      case "OVERCAST" -> new CloudTemperatureAdjustment(-2, 2, 0);
      case "STORM" -> new CloudTemperatureAdjustment(-3, 3, -1);
      default -> new CloudTemperatureAdjustment(0, 0, 0);
    };
  }

  private String crownLightCondition(CaravanWeatherProfile profile, GolarionDate date) {
    if (!profile.crownOfWorld()) {
      return null;
    }
    return switch (crownRegionBand(profile.elevation())) {
      case "OUTER_RIM" -> switch (date.month()) {
        case 1, 11, 12 -> POLAR_TWILIGHT;
        case 5, 6, 7 -> MIDNIGHT_SUN;
        default -> NORMAL;
      };
      case "HIGH_ICE" -> switch (date.month()) {
        case 1, 11 -> POLAR_TWILIGHT;
        case 12 -> POLAR_NIGHT;
        case 5, 6, 7 -> MIDNIGHT_SUN;
        default -> NORMAL;
      };
      case "BOREAL_EXPANSE" -> switch (date.month()) {
        case 1, 12 -> POLAR_NIGHT;
        case 2, 11 -> POLAR_TWILIGHT;
        case 5, 8 -> MIDNIGHT_SUN;
        case 6, 7 -> POLAR_DAY;
        default -> NORMAL;
      };
      default -> NORMAL;
    };
  }

  private String crownRegionBand(WeatherElevation elevation) {
    return switch (elevation) {
      case SEA_LEVEL, LOWLAND -> "OUTER_RIM";
      case HIGHLAND -> "HIGH_ICE";
      case PEAK -> "BOREAL_EXPANSE";
    };
  }

  private long hashSeed(
      UUID caravanId,
      GolarionDate date,
      CaravanWeatherProfile profile,
      CaravanWeatherForecastState previousState) {
    long seed = caravanId.getMostSignificantBits() ^ caravanId.getLeastSignificantBits();
    seed = 31 * seed + GolarionCalendar.toOffset(date);
    seed = 31 * seed + profile.climateBaseline().ordinal();
    seed = 31 * seed + profile.elevation().ordinal();
    seed = 31 * seed + (profile.crownOfWorld() ? 1 : 0);
    if (previousState != null) {
      seed = 31 * seed + previousState.targetTemperatureF();
      seed = 31 * seed + previousState.remainingTargetDays();
      seed = 31 * seed + previousState.dayBaseTemperatureF();
      seed = 31 * seed + previousState.nightBaseTemperatureF();
      seed = 31 * seed + previousState.carryOverRemainingPeriods();
      seed = 31 * seed + (previousState.carryOverPrecipitation() == null ? 0 : previousState.carryOverPrecipitation().hashCode());
    }
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
        toView(weather.duskToMidnight()),
        null);
  }

  private static WeatherSnapshotView toView(WeatherSnapshot weather, String crownLightCondition) {
    return new WeatherSnapshotView(
        toView(weather.midnightToDawn()),
        toView(weather.dawnToNoon()),
        toView(weather.noonToDusk()),
        toView(weather.duskToMidnight()),
        crownLightCondition);
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

  private static int fahrenheitToCelsius(int fahrenheit) {
    return Math.round((fahrenheit - 32) * 5 / 9.0f);
  }

  private record GeneratedWeather(
      CaravanWeatherSnapshot snapshot,
      CaravanWeatherForecastState forecastState) {}

  private record TemperatureTrendState(
      int targetTemperatureF,
      int remainingTargetDays,
      int dayBaseTemperatureF,
      int nightBaseTemperatureF) {}

  private record DailyPrecipitation(
      String precipitation,
      int startPeriod,
      int endPeriod,
      int carryOverRemainingPeriods,
      String cloudCover) {

    static DailyPrecipitation none() {
      return new DailyPrecipitation(NONE, -1, -1, 0, "CLEAR");
    }

    boolean affectsPeriod(int periodIndex) {
      return !NONE.equals(precipitation) && periodIndex >= startPeriod && periodIndex <= endPeriod;
    }

    boolean isFog() {
      return precipitation.contains("FOG") || precipitation.contains("MIST");
    }

    boolean isThunderstorm() {
      return precipitation.contains("THUNDER");
    }

    boolean isSnowStorm() {
      return precipitation.contains("SNOW");
    }

    String carryOverPrecipitation() {
      return carryOverRemainingPeriods > 0 ? precipitation : null;
    }
  }

  private record CloudTemperatureAdjustment(
      int dayOffsetF,
      int nightOffsetF,
      int transitionOffsetF) {}
}
