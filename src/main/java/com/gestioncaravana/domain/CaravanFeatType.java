package com.gestioncaravana.domain;

import java.util.List;

public record CaravanFeatType(
    String code,
    String name,
    String description,
    List<String> prerequisites,
    String benefitText,
    String specialText,
    String notes,
    boolean repeatable,
    int selectionLimit,
    Integer minimumLevel,
    String automationMode,
    String automationStateInputs,
    String automationExactAutomation) {

  public CaravanFeatType {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("code is required");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("description is required");
    }
    prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
    if (benefitText == null || benefitText.isBlank()) {
      throw new IllegalArgumentException("benefitText is required");
    }
    if (selectionLimit < 1) {
      throw new IllegalArgumentException("selectionLimit must be greater than or equal to 1");
    }
    if (minimumLevel != null && minimumLevel < 1) {
      throw new IllegalArgumentException("minimumLevel must be greater than or equal to 1");
    }
    automationMode = normalizeText(automationMode);
    automationStateInputs = normalizeText(automationStateInputs);
    automationExactAutomation = normalizeText(automationExactAutomation);
  }

  private static String normalizeText(String text) {
    if (text == null) {
      return null;
    }
    var trimmed = text.trim();
    return trimmed.isBlank() ? null : trimmed;
  }
}
