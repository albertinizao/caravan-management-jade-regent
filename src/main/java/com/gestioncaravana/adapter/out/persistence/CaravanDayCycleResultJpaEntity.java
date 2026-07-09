package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "caravan_day_cycle_results")
public class CaravanDayCycleResultJpaEntity {

  @Id
  @Column(length = 36, nullable = false, updatable = false)
  private String id;

  @Column(length = 36, nullable = false)
  private String caravanId;

  @Column(nullable = false)
  private String previewFingerprint;

  @Column(nullable = false)
  private int dayIndex;

  @Column(nullable = false)
  private Instant resolvedAt;

  @Column(nullable = false)
  private int startingSupplyUnits;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal startingPerishableFood;

  @Column(nullable = false)
  private int startingPerishableUnits;

  @Column(nullable = false)
  private int generatedSuppliesFromAgricultors;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal generatedAlchemyValueFromBoticarios;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal requiredConsumption;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal generatedFood;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal leftoverFood;

  @Column(nullable = false)
  private int finalSupplyUnits;

  @Column(nullable = false)
  private int finalPerishableUnits;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal finalPerishableFood;

  @Column(nullable = false)
  private boolean confirmed;

  @Lob
  @Column(nullable = false)
  private String simulationJson;

  @Lob
  @Column(nullable = false)
  private String warningsJson;

  protected CaravanDayCycleResultJpaEntity() {}

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

  public String getPreviewFingerprint() {
    return previewFingerprint;
  }

  public void setPreviewFingerprint(String previewFingerprint) {
    this.previewFingerprint = previewFingerprint;
  }

  public int getDayIndex() {
    return dayIndex;
  }

  public void setDayIndex(int dayIndex) {
    this.dayIndex = dayIndex;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public void setResolvedAt(Instant resolvedAt) {
    this.resolvedAt = resolvedAt;
  }

  public int getStartingSupplyUnits() {
    return startingSupplyUnits;
  }

  public void setStartingSupplyUnits(int startingSupplyUnits) {
    this.startingSupplyUnits = startingSupplyUnits;
  }

  public BigDecimal getStartingPerishableFood() {
    return startingPerishableFood;
  }

  public void setStartingPerishableFood(BigDecimal startingPerishableFood) {
    this.startingPerishableFood = startingPerishableFood;
  }

  public int getStartingPerishableUnits() {
    return startingPerishableUnits;
  }

  public void setStartingPerishableUnits(int startingPerishableUnits) {
    this.startingPerishableUnits = startingPerishableUnits;
  }

  public int getGeneratedSuppliesFromAgricultors() {
    return generatedSuppliesFromAgricultors;
  }

  public void setGeneratedSuppliesFromAgricultors(int generatedSuppliesFromAgricultors) {
    this.generatedSuppliesFromAgricultors = generatedSuppliesFromAgricultors;
  }

  public BigDecimal getGeneratedAlchemyValueFromBoticarios() {
    return generatedAlchemyValueFromBoticarios;
  }

  public void setGeneratedAlchemyValueFromBoticarios(BigDecimal generatedAlchemyValueFromBoticarios) {
    this.generatedAlchemyValueFromBoticarios = generatedAlchemyValueFromBoticarios;
  }

  public BigDecimal getRequiredConsumption() {
    return requiredConsumption;
  }

  public void setRequiredConsumption(BigDecimal requiredConsumption) {
    this.requiredConsumption = requiredConsumption;
  }

  public BigDecimal getGeneratedFood() {
    return generatedFood;
  }

  public void setGeneratedFood(BigDecimal generatedFood) {
    this.generatedFood = generatedFood;
  }

  public BigDecimal getLeftoverFood() {
    return leftoverFood;
  }

  public void setLeftoverFood(BigDecimal leftoverFood) {
    this.leftoverFood = leftoverFood;
  }

  public int getFinalSupplyUnits() {
    return finalSupplyUnits;
  }

  public void setFinalSupplyUnits(int finalSupplyUnits) {
    this.finalSupplyUnits = finalSupplyUnits;
  }

  public int getFinalPerishableUnits() {
    return finalPerishableUnits;
  }

  public void setFinalPerishableUnits(int finalPerishableUnits) {
    this.finalPerishableUnits = finalPerishableUnits;
  }

  public BigDecimal getFinalPerishableFood() {
    return finalPerishableFood;
  }

  public void setFinalPerishableFood(BigDecimal finalPerishableFood) {
    this.finalPerishableFood = finalPerishableFood;
  }

  public boolean isConfirmed() {
    return confirmed;
  }

  public void setConfirmed(boolean confirmed) {
    this.confirmed = confirmed;
  }

  public String getSimulationJson() {
    return simulationJson;
  }

  public void setSimulationJson(String simulationJson) {
    this.simulationJson = simulationJson;
  }

  public String getWarningsJson() {
    return warningsJson;
  }

  public void setWarningsJson(String warningsJson) {
    this.warningsJson = warningsJson;
  }
}
