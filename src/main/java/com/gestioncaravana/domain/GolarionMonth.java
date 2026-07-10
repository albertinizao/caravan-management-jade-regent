package com.gestioncaravana.domain;

public enum GolarionMonth {
  ABADIO(1, "Abadio", 31),
  CALISTRIL(2, "Calistril", 28),
  FARASTO(3, "Farasto", 31),
  GOZRAN(4, "Gozran", 30),
  DESNIO(5, "Desnio", 31),
  SARENITH(6, "Sarenith", 30),
  ERASTO(7, "Erasto", 31),
  ARODIO(8, "Arodio", 31),
  ROVA(9, "Rova", 30),
  LAMASHAN(10, "Lamashan", 31),
  NETH(11, "Neth", 30),
  KUTHONA(12, "Kuthona", 31);

  private final int value;
  private final String displayName;
  private final int standardLength;

  GolarionMonth(int value, String displayName, int standardLength) {
    this.value = value;
    this.displayName = displayName;
    this.standardLength = standardLength;
  }

  public int value() {
    return value;
  }

  public String displayName() {
    return displayName;
  }

  public int length(boolean leapYear) {
    if (this == CALISTRIL && leapYear) {
      return 29;
    }
    return standardLength;
  }

  public static GolarionMonth fromValue(int value) {
    for (var month : values()) {
      if (month.value == value) {
        return month;
      }
    }
    throw new IllegalArgumentException("Unsupported Golarion month: " + value);
  }
}
