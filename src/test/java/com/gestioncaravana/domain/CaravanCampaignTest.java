package com.gestioncaravana.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

  @Test
  void adjustsLevelAndDiscontentWithinTheAllowedBounds() {
    var now = Instant.parse("2026-01-01T00:00:00Z");
    var campaign = CaravanCampaign.create(UUID.randomUUID(), "Northern Run", null, now);

    var leveledUp = campaign.adjustLevel(1, now.plusSeconds(60));
    var moreDiscontent = leveledUp.adjustDiscontent(2, now.plusSeconds(120));
    var leveledDown = moreDiscontent.adjustLevel(-1, now.plusSeconds(180));
    var lessDiscontent = leveledDown.adjustDiscontent(-1, now.plusSeconds(240));

    assertThat(leveledUp.level()).isEqualTo(2);
    assertThat(moreDiscontent.discontent()).isEqualTo(2);
    assertThat(leveledDown.level()).isEqualTo(1);
    assertThat(lessDiscontent.discontent()).isEqualTo(1);
  }

  @Test
  void rejectsLevelDropsBelowOneAndNegativeDiscontent() {
    var now = Instant.parse("2026-01-01T00:00:00Z");
    var campaign = CaravanCampaign.create(UUID.randomUUID(), "Northern Run", null, now);

    assertThatThrownBy(() -> campaign.adjustLevel(-1, now))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("level must be greater than or equal to 1");
    assertThatThrownBy(() -> campaign.adjustDiscontent(-1, now))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("discontent must be greater than or equal to 0");
  }

  @Test
  void updatesMainStatsWhilePreservingTheTotalPointsBudget() {
    var now = Instant.parse("2026-01-01T00:00:00Z");
    var campaign = CaravanCampaign.create(UUID.randomUUID(), "Northern Run", null, now);

    var updated = campaign.updateMainStats(2, 1, 1, 1, now.plusSeconds(60));

    assertThat(updated.mainStats()).isEqualTo(new CaravanMainStats(2, 1, 1, 1, 2));
  }
}

