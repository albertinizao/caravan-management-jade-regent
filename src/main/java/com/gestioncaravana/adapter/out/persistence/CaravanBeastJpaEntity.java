package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "caravan_beasts")
public class CaravanBeastJpaEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private String id;

  @Column(nullable = false)
  private String caravanId;

  @Column(nullable = false)
  private String sourceType;

  @Column
  private String catalogBeastCode;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String size;

  @Column(nullable = false)
  private int strength;

  @Column(nullable = false)
  private int speed;

  @Column
  private Integer thermalAdaptation;

  @Column
  private Integer basePrice;

  @Column
  private Integer trainedPrice;

  @Column(nullable = false)
  private boolean fourLegged;

  @Column(nullable = false, length = 2000)
  private String specialNote;

  @Column(nullable = false, length = 4000)
  private String description;

  @Column(length = 4000)
  private String customNotes;

  @Column(nullable = true)
  private Integer consumption;

  @Column(length = 2000)
  private String availableRoleCodesCsv;

  @Column
  private String activeRoleCode;

  @Column(nullable = false)
  private String assignmentType;

  @Column
  private String assignedWagonId;

  @Column(precision = 4, scale = 1, nullable = true)
  private BigDecimal occupiedSpace;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected CaravanBeastJpaEntity() {}

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

  public String getCatalogBeastCode() {
    return catalogBeastCode;
  }

  public void setCatalogBeastCode(String catalogBeastCode) {
    this.catalogBeastCode = catalogBeastCode;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public int getStrength() {
    return strength;
  }

  public void setStrength(int strength) {
    this.strength = strength;
  }

  public int getSpeed() {
    return speed;
  }

  public void setSpeed(int speed) {
    this.speed = speed;
  }

  public Integer getThermalAdaptation() {
    return thermalAdaptation;
  }

  public void setThermalAdaptation(Integer thermalAdaptation) {
    this.thermalAdaptation = thermalAdaptation;
  }

  public Integer getBasePrice() {
    return basePrice;
  }

  public void setBasePrice(Integer basePrice) {
    this.basePrice = basePrice;
  }

  public Integer getTrainedPrice() {
    return trainedPrice;
  }

  public void setTrainedPrice(Integer trainedPrice) {
    this.trainedPrice = trainedPrice;
  }

  public boolean isFourLegged() {
    return fourLegged;
  }

  public void setFourLegged(boolean fourLegged) {
    this.fourLegged = fourLegged;
  }

  public String getSpecialNote() {
    return specialNote;
  }

  public void setSpecialNote(String specialNote) {
    this.specialNote = specialNote;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCustomNotes() {
    return customNotes;
  }

  public void setCustomNotes(String customNotes) {
    this.customNotes = customNotes;
  }

  public Integer getConsumption() {
    return consumption;
  }

  public void setConsumption(Integer consumption) {
    this.consumption = consumption;
  }

  public String getAvailableRoleCodesCsv() {
    return availableRoleCodesCsv;
  }

  public void setAvailableRoleCodesCsv(String availableRoleCodesCsv) {
    this.availableRoleCodesCsv = availableRoleCodesCsv;
  }

  public String getActiveRoleCode() {
    return activeRoleCode;
  }

  public void setActiveRoleCode(String activeRoleCode) {
    this.activeRoleCode = activeRoleCode;
  }

  public String getAssignmentType() {
    return assignmentType;
  }

  public void setAssignmentType(String assignmentType) {
    this.assignmentType = assignmentType;
  }

  public String getAssignedWagonId() {
    return assignedWagonId;
  }

  public void setAssignedWagonId(String assignedWagonId) {
    this.assignedWagonId = assignedWagonId;
  }

  public BigDecimal getOccupiedSpace() {
    return occupiedSpace;
  }

  public void setOccupiedSpace(BigDecimal occupiedSpace) {
    this.occupiedSpace = occupiedSpace;
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
