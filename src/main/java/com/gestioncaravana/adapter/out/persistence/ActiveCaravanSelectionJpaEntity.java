package com.gestioncaravana.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "active_caravan_selection")
public class ActiveCaravanSelectionJpaEntity {

  @Id
  private Long id;

  @Column(name = "caravan_id", length = 36)
  private String caravanId;

  protected ActiveCaravanSelectionJpaEntity() {}

  public static ActiveCaravanSelectionJpaEntity singleton() {
    var entity = new ActiveCaravanSelectionJpaEntity();
    entity.setId(1L);
    return entity;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCaravanId() {
    return caravanId;
  }

  public void setCaravanId(String caravanId) {
    this.caravanId = caravanId;
  }
}

