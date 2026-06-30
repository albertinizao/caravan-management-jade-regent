package com.gestioncaravana.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CaravanCampaignTest {

  @Test
  void createInitializesTheRuleBasedDefaultState() {
    var now = Instant.parse("2026-01-01T00:00:00Z");

    var campaign = CaravanCampaign.create(UUID.randomUUID(), "  Northern Run  ", "  First campaign  ", now);

    assertThat(campaign.name()).isEqualTo("Northern Run");
    assertThat(campaign.description()).isEqualTo("First campaign");
    assertThat(campaign.level()).isEqualTo(1);
    assertThat(campaign.mainStats()).isEqualTo(CaravanMainStats.initial());
    assertThat(campaign.discontent()).isZero();
    assertThat(campaign.status()).isEqualTo(CaravanCampaignStatus.ACTIVE);
    assertThat(campaign.createdAt()).isEqualTo(now);
    assertThat(campaign.updatedAt()).isEqualTo(now);
  }

  @Test
  void createAllowsAnExplicitInitialMainStatAllocation() {
    var now = Instant.parse("2026-01-01T00:00:00Z");

    var campaign = CaravanCampaign.create(
        UUID.randomUUID(),
        "Northern Run",
        null,
        CaravanMainStats.withInitialAllocation(2, 1, 1, 3),
        now);

    assertThat(campaign.mainStats()).isEqualTo(new CaravanMainStats(2, 1, 1, 3, 0));
  }
}

