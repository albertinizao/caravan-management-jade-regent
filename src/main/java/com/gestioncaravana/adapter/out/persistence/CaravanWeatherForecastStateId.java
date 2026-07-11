package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CaravanWeatherForecastStateId implements Serializable {

  @Column(length = 36, nullable = false)
  private String caravanId;

  @Column(name = "forecast_year", nullable = false)
  private int year;

  @Column(name = "forecast_month", nullable = false)
  private int month;

  @Column(name = "forecast_day", nullable = false)
  private int day;

  public CaravanWeatherForecastStateId() {}

  public CaravanWeatherForecastStateId(String caravanId, int year, int month, int day) {
    this.caravanId = caravanId;
    this.year = year;
    this.month = month;
    this.day = day;
  }

  public String getCaravanId() {
    return caravanId;
  }

  public void setCaravanId(String caravanId) {
    this.caravanId = caravanId;
  }

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }

  public int getMonth() {
    return month;
  }

  public void setMonth(int month) {
    this.month = month;
  }

  public int getDay() {
    return day;
  }

  public void setDay(int day) {
    this.day = day;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CaravanWeatherForecastStateId that)) {
      return false;
    }
    return year == that.year && month == that.month && day == that.day && Objects.equals(caravanId, that.caravanId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(caravanId, year, month, day);
  }
}
