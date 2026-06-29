package com.gestioncaravana.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TravelerContractTest {

  @Test
  void acceptsSalaryWithUpToTwoDecimals() {
    var contract = TravelerContract.create(BigDecimal.valueOf(12.34), "  Terms  ", Instant.parse("2026-01-01T00:00:00Z"));

    assertThat(contract).isNotNull();
    assertThat(contract.salary()).isEqualByComparingTo("12.34");
    assertThat(contract.conditions()).isEqualTo("Terms");
  }

  @Test
  void rejectsSalaryWithMoreThanTwoDecimals() {
    assertThatThrownBy(() -> TravelerContract.create(new BigDecimal("12.345"), "Terms", Instant.parse("2026-01-01T00:00:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at most 2 decimal places");
  }
}
