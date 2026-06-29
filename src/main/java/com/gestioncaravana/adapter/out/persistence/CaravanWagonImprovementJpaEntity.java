package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "caravan_wagon_improvements")
public class CaravanWagonImprovementJpaEntity {

  @Id
  @Column(length = 36, nullable = false, updatable = false)
  private String id;

  @Column(length = 36, nullable = false)
  private String caravanId;

  @Column(length = 36, nullable = false)
  private String wagonId;

  @Column(nullable = false)
  private String improvementTypeCode;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected CaravanWagonImprovementJpaEntity() {}

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

  public String getWagonId() {
    return wagonId;
  }

  public void setWagonId(String wagonId) {
    this.wagonId = wagonId;
  }

  public String getImprovementTypeCode() {
    return improvementTypeCode;
  }

  public void setImprovementTypeCode(String improvementTypeCode) {
    this.improvementTypeCode = improvementTypeCode;
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
