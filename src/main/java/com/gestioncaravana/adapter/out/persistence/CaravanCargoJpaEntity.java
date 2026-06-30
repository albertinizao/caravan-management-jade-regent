package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "caravan_cargo")
public class CaravanCargoJpaEntity {

  @Id
  @Column(length = 36, nullable = false, updatable = false)
  private String id;

  @Column(length = 36, nullable = false)
  private String caravanId;

  @Column(nullable = false)
  private String sourceType;

  @Column
  private String catalogCode;

  @Column(nullable = false)
  private String displayName;

  @Column(nullable = false)
  private String category;

  @Column(nullable = false)
  private int quantity;

  @Column(nullable = false)
  private int cargoUnits;

  @Column(length = 36)
  private String wagonId;

  @Column(length = 2000)
  private String origin;

  @Column(length = 2000)
  private String specificCommodity;

  @Column(length = 2000)
  private String deity;

  @Column(length = 4000)
  private String notes;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected CaravanCargoJpaEntity() {}

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

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  public String getCatalogCode() {
    return catalogCode;
  }

  public void setCatalogCode(String catalogCode) {
    this.catalogCode = catalogCode;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public int getCargoUnits() {
    return cargoUnits;
  }

  public void setCargoUnits(int cargoUnits) {
    this.cargoUnits = cargoUnits;
  }

  public String getWagonId() {
    return wagonId;
  }

  public void setWagonId(String wagonId) {
    this.wagonId = wagonId;
  }

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public String getSpecificCommodity() {
    return specificCommodity;
  }

  public void setSpecificCommodity(String specificCommodity) {
    this.specificCommodity = specificCommodity;
  }

  public String getDeity() {
    return deity;
  }

  public void setDeity(String deity) {
    this.deity = deity;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
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
