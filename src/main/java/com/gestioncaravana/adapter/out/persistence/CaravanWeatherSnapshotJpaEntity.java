package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "caravan_weather_snapshots")
public class CaravanWeatherSnapshotJpaEntity {

  @EmbeddedId
  private CaravanWeatherSnapshotId id;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "precipitation", column = @Column(name = "midnight_precipitation", length = 64)),
      @AttributeOverride(name = "windStrength", column = @Column(name = "midnight_wind_strength", length = 64)),
      @AttributeOverride(name = "temperatureC", column = @Column(name = "midnight_temperature_c")),
      @AttributeOverride(name = "temperatureF", column = @Column(name = "midnight_temperature_f"))
  })
  private WeatherPeriodEmbeddable midnightToDawn = new WeatherPeriodEmbeddable();

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "precipitation", column = @Column(name = "dawn_precipitation", length = 64)),
      @AttributeOverride(name = "windStrength", column = @Column(name = "dawn_wind_strength", length = 64)),
      @AttributeOverride(name = "temperatureC", column = @Column(name = "dawn_temperature_c")),
      @AttributeOverride(name = "temperatureF", column = @Column(name = "dawn_temperature_f"))
  })
  private WeatherPeriodEmbeddable dawnToNoon = new WeatherPeriodEmbeddable();

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "precipitation", column = @Column(name = "noon_precipitation", length = 64)),
      @AttributeOverride(name = "windStrength", column = @Column(name = "noon_wind_strength", length = 64)),
      @AttributeOverride(name = "temperatureC", column = @Column(name = "noon_temperature_c")),
      @AttributeOverride(name = "temperatureF", column = @Column(name = "noon_temperature_f"))
  })
  private WeatherPeriodEmbeddable noonToDusk = new WeatherPeriodEmbeddable();

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "precipitation", column = @Column(name = "dusk_precipitation", length = 64)),
      @AttributeOverride(name = "windStrength", column = @Column(name = "dusk_wind_strength", length = 64)),
      @AttributeOverride(name = "temperatureC", column = @Column(name = "dusk_temperature_c")),
      @AttributeOverride(name = "temperatureF", column = @Column(name = "dusk_temperature_f"))
  })
  private WeatherPeriodEmbeddable duskToMidnight = new WeatherPeriodEmbeddable();

  @Column(nullable = false)
  private Instant generatedAt;

  protected CaravanWeatherSnapshotJpaEntity() {}

  public CaravanWeatherSnapshotId getId() {
    return id;
  }

  public void setId(CaravanWeatherSnapshotId id) {
    this.id = id;
  }

  public WeatherPeriodEmbeddable getMidnightToDawn() {
    return midnightToDawn;
  }

  public void setMidnightToDawn(WeatherPeriodEmbeddable midnightToDawn) {
    this.midnightToDawn = midnightToDawn;
  }

  public WeatherPeriodEmbeddable getDawnToNoon() {
    return dawnToNoon;
  }

  public void setDawnToNoon(WeatherPeriodEmbeddable dawnToNoon) {
    this.dawnToNoon = dawnToNoon;
  }

  public WeatherPeriodEmbeddable getNoonToDusk() {
    return noonToDusk;
  }

  public void setNoonToDusk(WeatherPeriodEmbeddable noonToDusk) {
    this.noonToDusk = noonToDusk;
  }

  public WeatherPeriodEmbeddable getDuskToMidnight() {
    return duskToMidnight;
  }

  public void setDuskToMidnight(WeatherPeriodEmbeddable duskToMidnight) {
    this.duskToMidnight = duskToMidnight;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(Instant generatedAt) {
    this.generatedAt = generatedAt;
  }
}
