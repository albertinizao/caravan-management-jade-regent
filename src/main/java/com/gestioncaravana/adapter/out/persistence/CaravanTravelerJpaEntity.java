package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "caravan_travelers")
public class CaravanTravelerJpaEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private String id;

  @Column(nullable = false)
  private String caravanId;

  @Column(nullable = false)
  private String fullName;

  @Column(length = 2000)
  private String description;

  @Column(nullable = false, length = 2000)
  private String availableRoleCodesCsv;

  @Column(length = 2000)
  private String activeRoleCodesCsv;

  @Column(nullable = false)
  private String activeRoleCode;

  @Column
  private Integer maxActiveRoleCount;

  @Column
  private String servedTravelerId;

  @Column
  private Integer daysServing = 0;

  @Column
  private String wagonId;

  @Column
  private BigDecimal salary;

  @Column(length = 4000)
  private String contractConditions;

  @Column(nullable = false)
  private int consumption;

  @Column(nullable = false)
  private Boolean generatingFood = Boolean.FALSE;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected CaravanTravelerJpaEntity() {}

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

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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

  public String getActiveRoleCodesCsv() {
    return activeRoleCodesCsv;
  }

  public void setActiveRoleCodesCsv(String activeRoleCodesCsv) {
    this.activeRoleCodesCsv = activeRoleCodesCsv;
  }

  public Integer getMaxActiveRoleCount() {
    return maxActiveRoleCount;
  }

  public void setMaxActiveRoleCount(Integer maxActiveRoleCount) {
    this.maxActiveRoleCount = maxActiveRoleCount;
  }

  public String getServedTravelerId() {
    return servedTravelerId;
  }

  public void setServedTravelerId(String servedTravelerId) {
    this.servedTravelerId = servedTravelerId;
  }

  public Integer getDaysServing() {
    return daysServing;
  }

  public void setDaysServing(Integer daysServing) {
    this.daysServing = daysServing;
  }

  public String getWagonId() {
    return wagonId;
  }

  public void setWagonId(String wagonId) {
    this.wagonId = wagonId;
  }

  public BigDecimal getSalary() {
    return salary;
  }

  public void setSalary(BigDecimal salary) {
    this.salary = salary;
  }

  public String getContractConditions() {
    return contractConditions;
  }

  public void setContractConditions(String contractConditions) {
    this.contractConditions = contractConditions;
  }

  public int getConsumption() {
    return consumption;
  }

  public void setConsumption(int consumption) {
    this.consumption = consumption;
  }

  public Boolean getGeneratingFood() {
    return generatingFood;
  }

  public void setGeneratingFood(Boolean generatingFood) {
    this.generatingFood = generatingFood;
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
