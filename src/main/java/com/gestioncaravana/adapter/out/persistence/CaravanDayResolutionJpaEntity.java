package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "caravan_day_resolutions")
public class CaravanDayResolutionJpaEntity {

  @Id
  @Column(length = 36, nullable = false, updatable = false)
  private String id;

  @Column(length = 36, nullable = false)
  private String caravanId;

  @Column(nullable = false, unique = true)
  private String idempotencyKey;

  @Column(nullable = false)
  private int resolvedDayIndex;

  @Column(nullable = false)
  private Instant resolvedAt;

  @Column(nullable = false)
  private int startingReserve;

  @Column(nullable = false)
  private int endingReserve;

  @Column(nullable = false)
  private int totalConsumption;

  @Column(nullable = false)
  private int totalGeneration;

  @Column(nullable = false)
  private int netDelta;

  @Column(nullable = false)
  private int shortage;

  @jakarta.persistence.Lob
  private String cargoMovementSummary;

  @jakarta.persistence.Lob
  private String choicesSummary;

  @jakarta.persistence.Lob
  private String contributionsSummary;

  @jakarta.persistence.Lob
  private String warningsSummary;

  protected CaravanDayResolutionJpaEntity() {}

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

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public int getResolvedDayIndex() {
    return resolvedDayIndex;
  }

  public void setResolvedDayIndex(int resolvedDayIndex) {
    this.resolvedDayIndex = resolvedDayIndex;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public void setResolvedAt(Instant resolvedAt) {
    this.resolvedAt = resolvedAt;
  }

  public int getStartingReserve() {
    return startingReserve;
  }

  public void setStartingReserve(int startingReserve) {
    this.startingReserve = startingReserve;
  }

  public int getEndingReserve() {
    return endingReserve;
  }

  public void setEndingReserve(int endingReserve) {
    this.endingReserve = endingReserve;
  }

  public int getTotalConsumption() {
    return totalConsumption;
  }

  public void setTotalConsumption(int totalConsumption) {
    this.totalConsumption = totalConsumption;
  }

  public int getTotalGeneration() {
    return totalGeneration;
  }

  public void setTotalGeneration(int totalGeneration) {
    this.totalGeneration = totalGeneration;
  }

  public int getNetDelta() {
    return netDelta;
  }

  public void setNetDelta(int netDelta) {
    this.netDelta = netDelta;
  }

  public int getShortage() {
    return shortage;
  }

  public void setShortage(int shortage) {
    this.shortage = shortage;
  }

  public String getCargoMovementSummary() {
    return cargoMovementSummary;
  }

  public void setCargoMovementSummary(String cargoMovementSummary) {
    this.cargoMovementSummary = cargoMovementSummary;
  }

  public String getChoicesSummary() {
    return choicesSummary;
  }

  public void setChoicesSummary(String choicesSummary) {
    this.choicesSummary = choicesSummary;
  }

  public String getContributionsSummary() {
    return contributionsSummary;
  }

  public void setContributionsSummary(String contributionsSummary) {
    this.contributionsSummary = contributionsSummary;
  }

  public String getWarningsSummary() {
    return warningsSummary;
  }

  public void setWarningsSummary(String warningsSummary) {
    this.warningsSummary = warningsSummary;
  }
}
