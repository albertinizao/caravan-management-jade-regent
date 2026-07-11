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

  @Column
  private String crownRegion;

  @Column(name = "crown_of_world")
  private Boolean legacyCrownOfWorld;

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

  public String getCrownRegion() {
    return crownRegion;
  }

  public void setCrownRegion(String crownRegion) {
    this.crownRegion = crownRegion;
  }

  public Boolean getLegacyCrownOfWorld() {
    return legacyCrownOfWorld;
  }

  public void setLegacyCrownOfWorld(Boolean legacyCrownOfWorld) {
    this.legacyCrownOfWorld = legacyCrownOfWorld;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
