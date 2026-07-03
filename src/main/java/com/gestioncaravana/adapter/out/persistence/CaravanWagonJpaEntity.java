package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "caravan_wagons")
public class CaravanWagonJpaEntity {

  @Id
  @Column(length = 36, nullable = false, updatable = false)
  private String id;

  @Column(length = 36, nullable = false)
  private String caravanId;

  @Column(nullable = false)
  private String wagonTypeCode;

  @Column
  private String displayName;

  @Column
  private String specificCommodity;

  @Column
  private Integer currentHitPoints;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected CaravanWagonJpaEntity() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCaravanId() {
    return caravanId;
  }

  public void setCaravanId(String caravanId) {
    this.caravanId = caravanId;
  }

  public String getWagonTypeCode() {
    return wagonTypeCode;
  }

  public void setWagonTypeCode(String wagonTypeCode) {
    this.wagonTypeCode = wagonTypeCode;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getSpecificCommodity() {
    return specificCommodity;
  }

  public void setSpecificCommodity(String specificCommodity) {
    this.specificCommodity = specificCommodity;
  }

  public Integer getCurrentHitPoints() {
    return currentHitPoints;
  }

  public void setCurrentHitPoints(Integer currentHitPoints) {
    this.currentHitPoints = currentHitPoints;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
