package com.gestioncaravana.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestioncaravana.domain.CaravanSupplyState;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CaravanSupplyStateRepositoryAdapterTest {

  @Autowired
  private CaravanSupplyStateRepositoryAdapter repository;

  @Test
  void persistsSharedJobProductivityStateLongerThanTheDefaultVarcharLimit() {
    var caravanId = UUID.randomUUID();
    var longState = "batidor=0.25=" + UUID.randomUUID() + "," + UUID.randomUUID() + ","
        + UUID.randomUUID() + "," + UUID.randomUUID() + "|cocinero=0.5="
        + UUID.randomUUID() + "," + UUID.randomUUID() + "," + UUID.randomUUID() + ","
        + UUID.randomUUID() + "," + UUID.randomUUID();

    repository.save(new CaravanSupplyState(
        caravanId,
        12,
        8,
        4,
        6,
        Instant.parse("2026-07-08T20:22:36Z"),
        longState));

    assertThat(repository.findByCaravanId(caravanId))
        .hasValueSatisfying(state -> assertThat(state.sharedJobProductivityState()).isEqualTo(longState));
  }
}
