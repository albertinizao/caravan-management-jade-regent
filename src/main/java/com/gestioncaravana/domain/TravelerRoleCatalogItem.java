package com.gestioncaravana.domain;

public record TravelerRoleCatalogItem(
    String code,
    String name,
    String description,
    String requirements,
    boolean requiresTargetTraveler,
    TravelerRoleHelperBenefitMode helperBenefitMode,
    Integer helperPeriodDays) {

  public TravelerRoleCatalogItem(
      String code,
      String name,
      String description,
      String requirements,
      boolean requiresTargetTraveler) {
    this(code, name, description, requirements, requiresTargetTraveler, TravelerRoleHelperBenefitMode.NONE, null);
  }

  public TravelerRoleCatalogItem {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("code is required");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
    helperBenefitMode = helperBenefitMode == null ? TravelerRoleHelperBenefitMode.NONE : helperBenefitMode;
    if (helperBenefitMode == TravelerRoleHelperBenefitMode.PERIODIC) {
      if (helperPeriodDays == null || helperPeriodDays < 1) {
        throw new IllegalArgumentException("helperPeriodDays is required for periodic helper roles");
      }
      if (helperPeriodDays % 2 != 0) {
        throw new IllegalArgumentException("helperPeriodDays must be even for periodic helper roles");
      }
    } else if (helperPeriodDays != null) {
      throw new IllegalArgumentException("helperPeriodDays is only allowed for periodic helper roles");
    }
  }
}
