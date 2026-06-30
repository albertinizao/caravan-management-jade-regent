package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CaravanMainStatsEmbeddable {

  @Column(name = "offense", nullable = false)
  private int offense;

  @Column(name = "defense", nullable = false)
  private int defense;

  @Column(name = "mobility", nullable = false)
  private int mobility;

  @Column(name = "morale", nullable = false)
  private int morale;

  @Column(name = "unassigned_points", nullable = false)
  private int unassignedPoints;

  protected CaravanMainStatsEmbeddable() {}

  public int getOffense() {
    return offense;
  }

  public void setOffense(int offense) {
    this.offense = offense;
  }

  public int getDefense() {
    return defense;
  }

  public void setDefense(int defense) {
    this.defense = defense;
  }

  public int getMobility() {
    return mobility;
  }

  public void setMobility(int mobility) {
    this.mobility = mobility;
  }

  public int getMorale() {
    return morale;
  }

  public void setMorale(int morale) {
    this.morale = morale;
  }

  public int getUnassignedPoints() {
    return unassignedPoints;
  }

  public void setUnassignedPoints(int unassignedPoints) {
    this.unassignedPoints = unassignedPoints;
  }
}

