package com.gestioncaravana.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CaravanBeastTest {

  @Test
  void defaultsOccupiedSpaceToOneInCatalogItems() {
    var item = new CaravanBeastCatalogItem(
        "custom-code",
        "Bestia",
        10,
        20,
        "M",
        2,
        30,
        0,
        true,
        "Ninguno",
        "Descripción",
        null);

    assertThat(item.occupiedSpace()).isEqualByComparingTo("1");
  }

  @Test
  void acceptsFractionalOccupiedSpaceInHalfSteps() {
    var beast = CaravanBeast.createCustom(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Bestia",
        "M",
        2,
        30,
        null,
        null,
        null,
        true,
        "Ninguno",
        "Descripción",
        null,
        BigDecimal.valueOf(0.5),
        Instant.parse("2026-01-01T00:00:00Z"));

    assertThat(beast.occupiedSpace()).isEqualByComparingTo("0.5");
  }

  @Test
  void rejectsOccupiedSpaceAboveTheConfiguredMaximum() {
    assertThatThrownBy(() -> CaravanBeast.createCustom(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Bestia",
        "M",
        2,
        30,
        null,
        null,
        null,
        true,
        "Ninguno",
        "Descripción",
        null,
        BigDecimal.valueOf(4.5),
        Instant.parse("2026-01-01T00:00:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("less than or equal to 4");
  }

  @Test
  void assignsTravelerRoleDataToCustomBeasts() {
    var beast = CaravanBeast.createCustom(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Bestia",
        "M",
        2,
        30,
        null,
        null,
        null,
        true,
        "Ninguno",
        "Descripción",
        null,
        Instant.parse("2026-01-01T00:00:00Z"))
        .assignTraveler(
            UUID.randomUUID(),
            java.util.List.of("pasajero", "carretero"),
            "carretero",
            Instant.parse("2026-01-01T00:00:00Z"));

    assertThat(beast.assignmentType()).isEqualTo(CaravanBeastAssignmentType.TRAVELER);
    assertThat(beast.availableRoleCodes()).containsExactly("pasajero", "carretero");
    assertThat(beast.activeRoleCode()).isEqualTo("carretero");
  }

  @Test
  void defaultsCustomConsumptionAndOccupiedSpaceToOneWhenOmitted() {
    var beast = CaravanBeast.createCustom(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Bestia",
        "M",
        2,
        30,
        null,
        null,
        null,
        true,
        "Ninguno",
        "Descripción",
        null,
        Instant.parse("2026-01-01T00:00:00Z"));

    assertThat(beast.consumption()).isEqualTo(1);
    assertThat(beast.occupiedSpace()).isEqualByComparingTo("1");
  }

  @Test
  void updatesCustomConsumptionAndOccupiedSpaceWithoutChangingTheRest() {
    var original = CaravanBeast.createCustom(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Bestia",
        "M",
        2,
        30,
        null,
        null,
        null,
        true,
        "Ninguno",
        "Descripción",
        "Notas",
        BigDecimal.ONE,
        Instant.parse("2026-01-01T00:00:00Z"));

    var updated = original.updateCustomEconomy(3, BigDecimal.valueOf(2.5), Instant.parse("2026-01-02T00:00:00Z"));

    assertThat(updated.consumption()).isEqualTo(3);
    assertThat(updated.occupiedSpace()).isEqualByComparingTo("2.5");
    assertThat(updated.name()).isEqualTo(original.name());
    assertThat(updated.updatedAt()).isAfter(original.updatedAt());
  }

}
