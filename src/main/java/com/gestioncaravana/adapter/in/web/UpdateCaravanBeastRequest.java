package com.gestioncaravana.adapter.in.web;

import java.math.BigDecimal;

public record UpdateCaravanBeastRequest(
    Integer consumption,
    BigDecimal occupiedSpace) {}
