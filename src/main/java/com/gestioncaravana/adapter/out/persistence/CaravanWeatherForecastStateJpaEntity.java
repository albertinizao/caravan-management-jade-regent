package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "caravan_weather_forecast_states")
public class CaravanWeatherForecastStateJpaEntity {

  @EmbeddedId
  private CaravanWeatherForecastStateId id;

  @Column(nullable = false)
  private Integer targetTemperatureF;

  @Column(nullable = false)
  private Integer remainingTargetDays;

  @Column(nullable = false)
  private Integer dayBaseTemperatureF;

  @Column(nullable = false)
  private Integer nightBaseTemperatureF;

  @Column(length = 64)
  private String carryOverPrecipitation;

  @Column(nullable = false)
  private Integer carryOverRemainingPeriods;

  @Column(length = 64)
  private String severeEvent;

  @Column(nullable = false)
  private Instant generatedAt;

  protected CaravanWeatherForecastStateJpaEntity() {}

  public CaravanWeatherForecastStateId getId() {
    return id;
  }

  public void setId(CaravanWeatherForecastStateId id) {
    this.id = id;
  }

  public Integer getTargetTemperatureF() {
    return targetTemperatureF;
  }

  public void setTargetTemperatureF(Integer targetTemperatureF) {
    this.targetTemperatureF = targetTemperatureF;
  }

  public Integer getRemainingTargetDays() {
    return remainingTargetDays;
  }

  public void setRemainingTargetDays(Integer remainingTargetDays) {
    this.remainingTargetDays = remainingTargetDays;
  }

  public Integer getDayBaseTemperatureF() {
    return dayBaseTemperatureF;
  }

  public void setDayBaseTemperatureF(Integer dayBaseTemperatureF) {
    this.dayBaseTemperatureF = dayBaseTemperatureF;
  }

  public Integer getNightBaseTemperatureF() {
    return nightBaseTemperatureF;
  }

  public void setNightBaseTemperatureF(Integer nightBaseTemperatureF) {
    this.nightBaseTemperatureF = nightBaseTemperatureF;
  }

  public String getCarryOverPrecipitation() {
    return carryOverPrecipitation;
  }

  public void setCarryOverPrecipitation(String carryOverPrecipitation) {
    this.carryOverPrecipitation = carryOverPrecipitation;
  }

  public Integer getCarryOverRemainingPeriods() {
    return carryOverRemainingPeriods;
  }

  public void setCarryOverRemainingPeriods(Integer carryOverRemainingPeriods) {
    this.carryOverRemainingPeriods = carryOverRemainingPeriods;
  }

  public String getSevereEvent() {
    return severeEvent;
  }

  public void setSevereEvent(String severeEvent) {
    this.severeEvent = severeEvent;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(Instant generatedAt) {
    this.generatedAt = generatedAt;
  }
}
