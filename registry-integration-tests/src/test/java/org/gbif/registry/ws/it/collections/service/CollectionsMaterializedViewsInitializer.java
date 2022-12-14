package org.gbif.registry.ws.it.collections.service;

import org.gbif.registry.database.DBInitializer;

import java.sql.Connection;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;

import lombok.SneakyThrows;

public class CollectionsMaterializedViewsInitializer implements BeforeAllCallback, DBInitializer {

  private PostgreSQLContainer postgreSQLContainer;

  public CollectionsMaterializedViewsInitializer() {}

  public CollectionsMaterializedViewsInitializer(PostgreSQLContainer postgreSQLContainer) {
    this.postgreSQLContainer = postgreSQLContainer;
  }

  @SneakyThrows
  @Override
  public void init(Connection connection) {
    // create materialized view for testing
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/create_duplicates_views.sql"));
    connection.close();
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    init(postgreSQLContainer.createConnection(""));
  }
}
