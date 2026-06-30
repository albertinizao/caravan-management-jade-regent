package com.gestioncaravana;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "com.gestioncaravana",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class ArchitectureBoundariesTest {

  @ArchTest
  static final ArchRule layeredBoundaries =
      layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .layer("Domain").definedBy("com.gestioncaravana.domain..")
          .layer("Application").definedBy("com.gestioncaravana.application..")
          .layer("InboundAdapter").definedBy("com.gestioncaravana.adapter.in..")
          .layer("OutboundAdapter").definedBy("com.gestioncaravana.adapter.out..")
          .whereLayer("Domain").mayOnlyBeAccessedByLayers(
              "Application", "InboundAdapter", "OutboundAdapter")
          .whereLayer("Application").mayOnlyBeAccessedByLayers(
              "InboundAdapter", "OutboundAdapter")
          .whereLayer("InboundAdapter").mayNotBeAccessedByAnyLayer()
          .whereLayer("OutboundAdapter").mayNotBeAccessedByAnyLayer();
}
