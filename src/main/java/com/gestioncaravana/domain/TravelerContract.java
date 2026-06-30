package com.gestioncaravana.domain;

import java.time.Instant;
import java.math.BigDecimal;

public record TravelerContract(BigDecimal salary, String conditions, Instant startedAt, Instant endedAt) {

  public TravelerContract {
    if (salary != null && salary.signum() < 0) {
      throw new IllegalArgumentException("salary must be greater than or equal to 0");
    }
    if (startedAt == null) {
      throw new IllegalArgumentException("startedAt is required");
    }
    if (salary != null && salary.scale() > 2) {
      throw new IllegalArgumentException("salary can have at most 2 decimal places");
    }
  }

  public static TravelerContract create(BigDecimal salary, String conditions, Instant now) {
    if (salary == null && (conditions == null || conditions.isBlank())) {
      return null;
    }
    return new TravelerContract(salary, normalize(conditions), now, null);
  }

  private static String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
