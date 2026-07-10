package com.gestioncaravana.application.model;

import java.math.BigDecimal;
import java.util.List;

public record CaravanDayCycleLogEntryView(
    String section,
    String title,
    List<String> details,
    BigDecimal foodDelta) {}
