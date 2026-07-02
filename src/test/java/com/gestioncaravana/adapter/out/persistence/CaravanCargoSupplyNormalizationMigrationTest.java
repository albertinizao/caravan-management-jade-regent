package com.gestioncaravana.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestioncaravana.domain.CaravanCargoSourceType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManagerFactory;

@SpringJUnitConfig(CaravanCargoSupplyNormalizationMigrationTest.TestConfiguration.class)
@Transactional
class CaravanCargoSupplyNormalizationMigrationTest {

  @Autowired
  private SpringDataCaravanCargoRepository repository;

  @Autowired
  private CaravanCargoSupplyNormalizationMigration migration;

  @Configuration(proxyBeanMethods = false)
  @EnableTransactionManagement
  @EnableJpaRepositories(basePackageClasses = SpringDataCaravanCargoRepository.class)
  @Import(CaravanCargoSupplyNormalizationMigration.class)
  static class TestConfiguration {

    @Bean
    DriverManagerDataSource dataSource() {
      var dataSource = new DriverManagerDataSource();
      dataSource.setDriverClassName("org.h2.Driver");
      dataSource.setUrl("jdbc:h2:mem:caravan-cargo-normalization-test;DB_CLOSE_DELAY=-1");
      dataSource.setUsername("sa");
      dataSource.setPassword("");
      return dataSource;
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DriverManagerDataSource dataSource) {
      var factoryBean = new LocalContainerEntityManagerFactoryBean();
      factoryBean.setDataSource(dataSource);
      factoryBean.setPackagesToScan("com.gestioncaravana.adapter.out.persistence");
      factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      factoryBean.setJpaPropertyMap(
          Map.of(
              "hibernate.hbm2ddl.auto", "create-drop",
              "hibernate.show_sql", "false"));
      return factoryBean;
    }

    @Bean
    PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
      return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);
    }
  }

  @Test
  @DisplayName("normalizeLegacySupplyStacks should split aggregated supply rows into unit rows")
  void normalizeLegacySupplyStacks_shouldSplitAggregatedSupplyRowsIntoUnitRows() {
    var caravanId = UUID.randomUUID().toString();
    var wagonId = UUID.randomUUID().toString();
    var createdAt = Instant.parse("2026-06-30T12:00:00Z");
    var entity = new CaravanCargoJpaEntity();
    entity.setId(UUID.randomUUID().toString());
    entity.setCaravanId(caravanId);
    entity.setSourceType(CaravanCargoSourceType.CATALOG.name());
    entity.setCatalogCode("suministros-perecederos");
    entity.setDisplayName("Suministros Perecederos");
    entity.setCategory("Artículos de mercancía");
    entity.setQuantity(3);
    entity.setCargoUnits(1);
    entity.setCurrentProvisions(23);
    entity.setDayPassed(true);
    entity.setWagonId(wagonId);
    entity.setCreatedAt(createdAt);
    entity.setUpdatedAt(createdAt);

    repository.save(entity);

    migration.normalizeLegacySupplyStacks();

    var normalized = repository.findAllByCaravanId(caravanId);

    assertThat(normalized).hasSize(3);
    assertThat(normalized).allSatisfy(row -> {
      assertThat(row.getQuantity()).isEqualTo(1);
      assertThat(row.getCargoUnits()).isEqualTo(1);
      assertThat(row.getCurrentProvisions()).isGreaterThan(0);
      assertThat(row.getDayPassed()).isTrue();
    });
    assertThat(normalized).extracting(CaravanCargoJpaEntity::getCurrentProvisions).containsExactlyInAnyOrder(8, 8, 7);
    assertThat(normalized).extracting(CaravanCargoJpaEntity::getId).contains(entity.getId());
  }
}
