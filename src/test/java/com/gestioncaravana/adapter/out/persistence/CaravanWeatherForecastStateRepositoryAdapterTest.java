package com.gestioncaravana.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestioncaravana.domain.CaravanWeatherForecastState;
import com.gestioncaravana.domain.GolarionDate;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CaravanWeatherForecastStateRepositoryAdapterTest {

  @Autowired
  private CaravanWeatherForecastStateRepositoryAdapter repository;

  @Test
  void persistsAndFindsForecastStateByCaravanIdAndDate() {
    var caravanId = UUID.randomUUID();
    var date = new GolarionDate(4712, 4, 12);

    repository.save(new CaravanWeatherForecastState(
        caravanId,
        date,
        78,
        3,
        82,
        66,
        "LIGHT_RAIN",
        2,
        "THUNDERSTORM",
        Instant.parse("2026-07-11T12:00:00Z")));

    assertThat(repository.findByCaravanIdAndDate(caravanId, date))
        .hasValueSatisfying(state -> {
          assertThat(state.targetTemperatureF()).isEqualTo(78);
          assertThat(state.remainingTargetDays()).isEqualTo(3);
          assertThat(state.dayBaseTemperatureF()).isEqualTo(82);
          assertThat(state.nightBaseTemperatureF()).isEqualTo(66);
          assertThat(state.carryOverPrecipitation()).isEqualTo("LIGHT_RAIN");
          assertThat(state.carryOverRemainingPeriods()).isEqualTo(2);
          assertThat(state.severeEvent()).isEqualTo("THUNDERSTORM");
        });
  }

  @Test
  void deletesForecastStateByExactDate() {
    var caravanId = UUID.randomUUID();
    var firstDate = new GolarionDate(4712, 4, 10);
    var secondDate = new GolarionDate(4712, 4, 11);
    repository.save(state(caravanId, firstDate, 70));
    repository.save(state(caravanId, secondDate, 71));

    repository.deleteByCaravanIdAndDate(caravanId, firstDate);

    assertThat(repository.findByCaravanIdAndDate(caravanId, firstDate)).isEmpty();
    assertThat(repository.findByCaravanIdAndDate(caravanId, secondDate)).isPresent();
  }

  @Test
  void deletesForecastStateFromEffectiveDateOnly() {
    var caravanId = UUID.randomUUID();
    var earlierDate = new GolarionDate(4712, 4, 10);
    var effectiveDate = new GolarionDate(4712, 4, 11);
    var laterDate = new GolarionDate(4712, 4, 12);
    repository.save(state(caravanId, earlierDate, 70));
    repository.save(state(caravanId, effectiveDate, 71));
    repository.save(state(caravanId, laterDate, 72));

    repository.deleteFromDate(caravanId, effectiveDate);

    assertThat(repository.findByCaravanIdAndDate(caravanId, earlierDate)).isPresent();
    assertThat(repository.findByCaravanIdAndDate(caravanId, effectiveDate)).isEmpty();
    assertThat(repository.findByCaravanIdAndDate(caravanId, laterDate)).isEmpty();
  }

  private CaravanWeatherForecastState state(UUID caravanId, GolarionDate date, int targetTemperatureF) {
    return new CaravanWeatherForecastState(
        caravanId,
        date,
        targetTemperatureF,
        1,
        targetTemperatureF + 4,
        targetTemperatureF - 6,
        null,
        0,
        null,
        Instant.parse("2026-07-11T12:00:00Z"));
  }
}
