package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "caravan_feats")
public class CaravanFeatJpaEntity {

  @Id
  @Column(length = 36, nullable = false, updatable = false)
  private String id;

  @Column(length = 36, nullable = false)
  private String caravanId;

  @Column(nullable = false)
  private String featTypeCode;

  @Column(nullable = false)
  private String acquisitionSourceType;

  @Column
  private Integer acquisitionLevel;

  @Column(length = 1000)
  private String acquisitionCause;

  @Column(nullable = false)
  private int selectionIndex;

  @Column
  private Boolean active;

  @Column
  private Boolean manualApplies;

  @Column(length = 1000)
  private String manualAppliesReason;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected CaravanFeatJpaEntity() {}

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

  public String getFeatTypeCode() {
    return featTypeCode;
  }

  public void setFeatTypeCode(String featTypeCode) {
    this.featTypeCode = featTypeCode;
  }

  public String getAcquisitionSourceType() {
    return acquisitionSourceType;
  }

  public void setAcquisitionSourceType(String acquisitionSourceType) {
    this.acquisitionSourceType = acquisitionSourceType;
  }

  public Integer getAcquisitionLevel() {
    return acquisitionLevel;
  }

  public void setAcquisitionLevel(Integer acquisitionLevel) {
    this.acquisitionLevel = acquisitionLevel;
  }

  public String getAcquisitionCause() {
    return acquisitionCause;
  }

  public void setAcquisitionCause(String acquisitionCause) {
    this.acquisitionCause = acquisitionCause;
  }

  public int getSelectionIndex() {
    return selectionIndex;
  }

  public void setSelectionIndex(int selectionIndex) {
    this.selectionIndex = selectionIndex;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public Boolean getManualApplies() {
    return manualApplies;
  }

  public void setManualApplies(Boolean manualApplies) {
    this.manualApplies = manualApplies;
  }

  public String getManualAppliesReason() {
    return manualAppliesReason;
  }

  public void setManualAppliesReason(String manualAppliesReason) {
    this.manualAppliesReason = manualAppliesReason;
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
