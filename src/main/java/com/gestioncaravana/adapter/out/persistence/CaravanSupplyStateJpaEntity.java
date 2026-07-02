package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "caravan_supply_states")
public class CaravanSupplyStateJpaEntity {

  @Id
  @Column(length = 36, nullable = false, updatable = false)
  private String caravanId;

  @Column(nullable = false)
  private Integer provisionReserve;

  @Column
  private Integer standardReserve;

  @Column
  private Integer perishableReserve;

  @Column(nullable = false)
  private Integer daysPassed;

  @Column(nullable = false)
  private Instant updatedAt;

  protected CaravanSupplyStateJpaEntity() {}

  public String getCaravanId() {
    return caravanId;
  }

  public void setCaravanId(String caravanId) {
    this.caravanId = caravanId;
  }

  public Integer getProvisionReserve() {
    return provisionReserve;
  }

  public void setProvisionReserve(Integer provisionReserve) {
    this.provisionReserve = provisionReserve;
  }

  public Integer getStandardReserve() {
    return standardReserve;
  }

  public void setStandardReserve(Integer standardReserve) {
    this.standardReserve = standardReserve;
  }

  public Integer getPerishableReserve() {
    return perishableReserve;
  }

  public void setPerishableReserve(Integer perishableReserve) {
    this.perishableReserve = perishableReserve;
  }

  public Integer getDaysPassed() {
    return daysPassed;
  }

  public void setDaysPassed(Integer daysPassed) {
    this.daysPassed = daysPassed;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
