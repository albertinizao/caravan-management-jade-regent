package com.gestioncaravana.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import com.gestioncaravana.domain.CaravanFeat;
import com.gestioncaravana.domain.CaravanFeatAcquisitionSourceType;
import java.time.Instant;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringJUnitConfig(CaravanFeatRepositoryAdapterTest.TestConfiguration.class)
@Transactional
class CaravanFeatRepositoryAdapterTest {

  @Autowired
  private CaravanFeatRepositoryAdapter adapter;

  @Configuration(proxyBeanMethods = false)
  @EnableTransactionManagement
  @EnableJpaRepositories(basePackageClasses = SpringDataCaravanFeatRepository.class)
  @Import(CaravanFeatRepositoryAdapter.class)
  static class TestConfiguration {

    @Bean
    DriverManagerDataSource dataSource() {
      var dataSource = new DriverManagerDataSource();
      dataSource.setDriverClassName("org.h2.Driver");
      dataSource.setUrl("jdbc:h2:mem:caravan-feat-repository-test;DB_CLOSE_DELAY=-1");
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
  }

  @Test
  @DisplayName("save should persist and roundtrip owned feat acquisition metadata")
  void save_shouldPersistAndRoundtripOwnedFeatAcquisitionMetadata() {
    var caravanId = UUID.randomUUID();
    var featId = UUID.randomUUID();
    var createdAt = Instant.parse("2026-06-30T10:15:30Z");
    var feat = new CaravanFeat(
        featId,
        caravanId,
        "improved-initiative",
        CaravanFeatAcquisitionSourceType.LEVEL_UP,
        3,
        null,
        1,
        true,
        createdAt,
        createdAt);

    var saved = adapter.save(feat);
    var found = adapter.findById(caravanId, featId);

    assertThat(saved).isEqualTo(feat);
    assertThat(found).contains(feat);
  }

  @Test
  @DisplayName("findAllByCaravanId should return only the caravan feats ordered by selection index")
  void findAllByCaravanId_shouldReturnOnlyCaravanFeatsOrderedBySelectionIndex() {
    var caravanId = UUID.randomUUID();
    var otherCaravanId = UUID.randomUUID();
    var baseTime = Instant.parse("2026-06-30T12:00:00Z");

    var laterSelection = new CaravanFeat(
        UUID.randomUUID(),
        caravanId,
        "endurance",
        CaravanFeatAcquisitionSourceType.OTHER,
        null,
        "Bonus training",
        2,
        true,
        baseTime.plusSeconds(60),
        baseTime.plusSeconds(60));
    var firstSelection = new CaravanFeat(
        UUID.randomUUID(),
        caravanId,
        "fleet",
        CaravanFeatAcquisitionSourceType.LEVEL_UP,
        1,
        null,
        1,
        true,
        baseTime,
        baseTime);
    var otherCaravanFeat = new CaravanFeat(
        UUID.randomUUID(),
        otherCaravanId,
        "improved-initiative",
        CaravanFeatAcquisitionSourceType.LEVEL_UP,
        2,
        null,
        1,
        true,
        baseTime.plusSeconds(120),
        baseTime.plusSeconds(120));

    adapter.save(laterSelection);
    adapter.save(firstSelection);
    adapter.save(otherCaravanFeat);

    var feats = adapter.findAllByCaravanId(caravanId);

    assertThat(feats).containsExactly(firstSelection, laterSelection);
    assertThat(feats).extracting(CaravanFeat::caravanId).containsOnly(caravanId);
  }

  @Test
  @DisplayName("save should update an existing owned feat without changing its identity")
  void save_shouldUpdateExistingOwnedFeatWithoutChangingIdentity() {
    var caravanId = UUID.randomUUID();
    var featId = UUID.randomUUID();
    var createdAt = Instant.parse("2026-06-30T14:45:00Z");
    var original = new CaravanFeat(
        featId,
        caravanId,
        "fleet",
        CaravanFeatAcquisitionSourceType.LEVEL_UP,
        2,
        null,
        1,
        true,
        createdAt,
        createdAt);

    adapter.save(original);

    var updated = new CaravanFeat(
        featId,
        caravanId,
        "fleet",
        CaravanFeatAcquisitionSourceType.OTHER,
        null,
        "Retcon after audit",
        1,
        false,
        createdAt,
        createdAt.plusSeconds(300));

    var saved = adapter.save(updated);
    var found = adapter.findById(caravanId, featId);
    var feats = adapter.findAllByCaravanId(caravanId);

    assertThat(saved).isEqualTo(updated);
    assertThat(found).contains(updated);
    assertThat(feats).containsExactly(updated);
    assertThat(adapter.countByCaravanIdAndFeatTypeCode(caravanId, "fleet")).isEqualTo(1);
  }
}
