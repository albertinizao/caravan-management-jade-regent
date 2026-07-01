package com.gestioncaravana.adapter.out.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarkdownCaravanFeatCatalogAdapterTest {

  @Test
  void loadsFeatCatalogFromRulesDocument() {
    var adapter = new MarkdownCaravanFeatCatalogAdapter();

    assertThat(adapter.all())
        .extracting("code")
        .contains("caravana-mejorada", "caravana-santificada", "carros-adicionales");
  }
}
