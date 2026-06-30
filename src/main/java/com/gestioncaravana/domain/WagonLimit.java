package com.gestioncaravana.domain;

public record WagonLimit(
    WagonLimitKind kind,
    Integer fixedMax,
    Integer ratioDenominator,
    String label) {

  public WagonLimit {
    if (kind == null) {
      throw new IllegalArgumentException("kind is required");
    }
    if (label == null || label.isBlank()) {
      throw new IllegalArgumentException("label is required");
    }
    if (kind == WagonLimitKind.FIXED && (fixedMax == null || fixedMax < 1)) {
      throw new IllegalArgumentException("fixedMax is required for fixed limits");
    }
    if (kind == WagonLimitKind.RATIO_OF_CARAVAN_CAPACITY
        && (ratioDenominator == null || ratioDenominator < 1)) {
      throw new IllegalArgumentException("ratioDenominator is required for ratio limits");
    }
    if (kind == WagonLimitKind.UNLIMITED && (fixedMax != null || ratioDenominator != null)) {
      throw new IllegalArgumentException("unlimited limits cannot define bounds");
    }
  }

  public static WagonLimit unlimited(String label) {
    return new WagonLimit(WagonLimitKind.UNLIMITED, null, null, label);
  }

  public static WagonLimit fixed(int max, String label) {
    return new WagonLimit(WagonLimitKind.FIXED, max, null, label);
  }

  public static WagonLimit ratio(int denominator, String label) {
    return new WagonLimit(WagonLimitKind.RATIO_OF_CARAVAN_CAPACITY, null, denominator, label);
  }

  public int resolveMaxAllowed(int caravanWagonCapacity) {
    return switch (kind) {
      case UNLIMITED -> Integer.MAX_VALUE;
      case FIXED -> fixedMax;
      case RATIO_OF_CARAVAN_CAPACITY -> Math.max(1, caravanWagonCapacity / ratioDenominator);
    };
  }
}
