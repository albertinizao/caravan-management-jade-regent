package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CaravanWeatherSnapshotId implements Serializable {

  @Column(length = 36, nullable = false)
  private String caravanId;

  @Column(name = "snapshot_year", nullable = false)
  private int year;

  @Column(name = "snapshot_month", nullable = false)
  private int month;

  @Column(name = "snapshot_day", nullable = false)
  private int weatherDay;

  public CaravanWeatherSnapshotId() {}

  public CaravanWeatherSnapshotId(String caravanId, int year, int month, int day) {
    this.caravanId = caravanId;
    this.year = year;
    this.month = month;
    this.weatherDay = day;
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
    return weatherDay;
  }

  public void setDay(int day) {
    this.weatherDay = day;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CaravanWeatherSnapshotId that)) {
      return false;
    }
    return year == that.year && month == that.month && weatherDay == that.weatherDay && Objects.equals(caravanId, that.caravanId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(caravanId, year, month, weatherDay);
  }
}
