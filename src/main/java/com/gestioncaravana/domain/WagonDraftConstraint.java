package com.gestioncaravana.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record WagonDraftConstraint(
    int maxLargeBeasts,
    int maxMediumBeasts,
    int minimumStrength) {

  private static final Pattern DRAFT_PATTERN = Pattern.compile(
      "(?i)\\s*(\\d+)\\s+criatura[s]?\\s+grande[s]?\\s*/\\s*(\\d+)\\s+mediana[s]?\\s*\\(\\+(\\d+)\\s+fuerza\\)\\s*");

  public WagonDraftConstraint {
    if (maxLargeBeasts < 0) {
      throw new IllegalArgumentException("maxLargeBeasts must be greater than or equal to 0");
    }
    if (maxMediumBeasts < 0) {
      throw new IllegalArgumentException("maxMediumBeasts must be greater than or equal to 0");
    }
    if (minimumStrength < 0) {
      throw new IllegalArgumentException("minimumStrength must be greater than or equal to 0");
    }
  }

  public static WagonDraftConstraint parse(String propulsion) {
    if (propulsion == null || propulsion.isBlank()) {
      throw new IllegalArgumentException("propulsion is required");
    }

    var matcher = DRAFT_PATTERN.matcher(propulsion);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Unsupported wagon propulsion format: " + propulsion);
    }

    return new WagonDraftConstraint(
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        Integer.parseInt(matcher.group(3)));
  }

  public int mediumSlotCapacity() {
    return maxMediumBeasts;
  }

  public int largeSlotCapacity() {
    return maxLargeBeasts;
  }
}
