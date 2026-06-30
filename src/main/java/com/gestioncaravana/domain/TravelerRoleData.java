package com.gestioncaravana.domain;

import java.util.UUID;

public record TravelerRoleData(UUID servedTravelerId) {
  public static TravelerRoleData empty() {
    return new TravelerRoleData(null);
  }
}
