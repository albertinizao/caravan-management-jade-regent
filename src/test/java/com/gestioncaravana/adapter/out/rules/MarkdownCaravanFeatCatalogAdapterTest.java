package com.gestioncaravana.adapter.out.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarkdownCaravanFeatCatalogAdapterTest {

  @Test
  void loadsFeatCatalogFromRulesDocument() {
    var adapter = new MarkdownCaravanFeatCatalogAdapter();
    var featsByCode =
        adapter.all().stream().collect(java.util.stream.Collectors.toMap(feat -> feat.code(), feat -> feat));

    assertThat(adapter.all())
        .extracting("code")
        .contains("caravana-mejorada", "caravana-santificada", "carros-adicionales");

    assertThat(featsByCode.get("cuidado-de-animales"))
        .satisfies(
            feat -> {
              assertThat(feat.description())
                  .isEqualTo(
                      "Perder a las bestias de tiro puede ser fatal para una caravana, por lo que habéis aprendido a tratar sus heridas con más eficacia.");
              assertThat(feat.prerequisites()).containsExactly("Seguridad 3.");
              assertThat(feat.benefitText())
                  .isEqualTo(
                      "Cada día que la caravana pase descansando, la podrá llevar a cabo una prueba especial de Seguridad gastando 1 unidad de suministros. Esta prueba permite recuperar 15 x nivel de la caravana PG a repartir entre las bestias de tiro como se considere.");
              assertThat(feat.specialText()).isNull();
              assertThat(feat.selectionLimit()).isEqualTo(1);
              assertThat(feat.automationMode()).isEqualTo("rest-day action");
              assertThat(feat.automationStateInputs()).contains("restCareUsedAt");
              assertThat(feat.automationExactAutomation()).contains("15 × caravanLevel");
            });

    assertThat(featsByCode.get("oferta-gancho").selectionLimit()).isEqualTo(2);
    assertThat(featsByCode.get("organizacion-impecable").selectionLimit()).isEqualTo(2);
    assertThat(featsByCode.get("consumo-eficiente").selectionLimit()).isEqualTo(3);
    assertThat(featsByCode.get("caravana-santificada").selectionLimit()).isEqualTo(999);
    assertThat(featsByCode.get("caravana-santificada").automationMode()).isEqualTo("manual");
  }
}
