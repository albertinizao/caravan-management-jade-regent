package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "caravan_weather_profiles")
public class CaravanWeatherProfileJpaEntity {

  @Id
  @Column(length = 36, nullable = false, updatable = false)
  private String caravanId;

  @Column(nullable = false)
  private String climateBaseline;

  @Column(nullable = false)
  private String elevation;

  @Column(nullable = false)
  private boolean crownOfWorld;

  @Column(nullable = false)
  private Instant updatedAt;

  protected CaravanWeatherProfileJpaEntity() {}

  public String getCaravanId() {
    return caravanId;
  }

  public void setCaravanId(String caravanId) {
    this.caravanId = caravanId;
  }

  public String getClimateBaseline() {
    return climateBaseline;
  }

  public void setClimateBaseline(String climateBaseline) {
    this.climateBaseline = climateBaseline;
  }

  public String getElevation() {
    return elevation;
  }

  public void setElevation(String elevation) {
    this.elevation = elevation;
  }

  public boolean isCrownOfWorld() {
    return crownOfWorld;
  }

  public void setCrownOfWorld(boolean crownOfWorld) {
    this.crownOfWorld = crownOfWorld;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
