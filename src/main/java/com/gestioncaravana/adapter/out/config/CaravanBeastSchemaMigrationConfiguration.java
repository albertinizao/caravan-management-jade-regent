package com.gestioncaravana.adapter.out.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class CaravanBeastSchemaMigrationConfiguration {

  @Bean
  ApplicationRunner caravanBeastSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
    return args -> {
      addColumnIfMissing(jdbcTemplate, "caravan_beasts", "consumption", "INT");
      addColumnIfMissing(jdbcTemplate, "caravan_beasts", "occupied_space", "DECIMAL(4,1)");
      backfillDefaults(jdbcTemplate, "caravan_beasts", "consumption", "1");
      backfillDefaults(jdbcTemplate, "caravan_beasts", "occupied_space", "1");
      setDefaultAndNotNull(jdbcTemplate, "caravan_beasts", "consumption", "1");
      setDefaultAndNotNull(jdbcTemplate, "caravan_beasts", "occupied_space", "1");
    };
  }

  private void addColumnIfMissing(JdbcTemplate jdbcTemplate, String tableName, String columnName, String ddl) throws SQLException {
    DataSource dataSource = Objects.requireNonNull(jdbcTemplate.getDataSource(), "dataSource is required");
    try (Connection connection = dataSource.getConnection()) {
      if (columnExists(connection, tableName, columnName)) {
        return;
      }
    }
    jdbcTemplate.execute("ALTER TABLE " + tableName.toUpperCase(Locale.ROOT) + " ADD COLUMN IF NOT EXISTS " + columnName.toUpperCase(Locale.ROOT) + " " + ddl);
  }

  private void backfillDefaults(JdbcTemplate jdbcTemplate, String tableName, String columnName, String defaultValue) {
    jdbcTemplate.update(
        "UPDATE " + tableName.toUpperCase(Locale.ROOT) + " SET " + columnName.toUpperCase(Locale.ROOT) + " = ? WHERE " + columnName.toUpperCase(Locale.ROOT) + " IS NULL",
        defaultValue);
  }

  private void setDefaultAndNotNull(JdbcTemplate jdbcTemplate, String tableName, String columnName, String defaultValue) {
    jdbcTemplate.execute(
        "ALTER TABLE " + tableName.toUpperCase(Locale.ROOT) + " ALTER COLUMN " + columnName.toUpperCase(Locale.ROOT) + " SET DEFAULT " + defaultValue);
    jdbcTemplate.execute(
        "ALTER TABLE " + tableName.toUpperCase(Locale.ROOT) + " ALTER COLUMN " + columnName.toUpperCase(Locale.ROOT) + " SET NOT NULL");
  }

  private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
    var metaData = connection.getMetaData();
    try (var columns = metaData.getColumns(null, null, tableName.toUpperCase(Locale.ROOT), columnName.toUpperCase(Locale.ROOT))) {
      if (columns.next()) {
        return true;
      }
    }
    try (var columns = metaData.getColumns(null, null, tableName.toUpperCase(Locale.ROOT), columnName.toLowerCase(Locale.ROOT))) {
      return columns.next();
    }
  }
}
