package com.gestioncaravana.domain;

public enum GolarionDayOfWeek {
  MOONDAY(0, "Día de la Luna", "Lun"),
  TOILDAY(1, "Día del Trabajo", "Tra"),
  WEALDAY(2, "Día de la Fortuna", "For"),
  OATHDAY(3, "Día del Juramento", "Jur"),
  FIREDAY(4, "Día del Fuego", "Fue"),
  STARDAY(5, "Día de las Estrellas", "Est"),
  SUNDAY(6, "Día del Sol", "Sol");

  private final int index;
  private final String displayName;
  private final String abbreviation;

  GolarionDayOfWeek(int index, String displayName, String abbreviation) {
    this.index = index;
    this.displayName = displayName;
    this.abbreviation = abbreviation;
  }

  public int index() {
    return index;
  }

  public String displayName() {
    return displayName;
  }

  public String abbreviation() {
    return abbreviation;
  }

  public static GolarionDayOfWeek fromIndex(int index) {
    var normalized = Math.floorMod(index, 7);
    for (var day : values()) {
      if (day.index == normalized) {
        return day;
      }
    }
    throw new IllegalArgumentException("Unsupported weekday index: " + index);
  }
}
