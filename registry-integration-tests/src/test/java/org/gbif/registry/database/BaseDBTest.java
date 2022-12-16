package org.gbif.registry.database;

import org.gbif.registry.ws.it.fixtures.TestConstants;

import java.sql.SQLException;
import java.time.Duration;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

public class BaseDBTest {

  public static final PostgreSQLContainer PG_CONTAINER;

  static {
    PG_CONTAINER = new PostgreSQLContainer("postgres:11.1").withDatabaseName("registry");
    PG_CONTAINER.withReuse(true).withLabel("reuse.tag", "registry_ITs_PG_container");
    PG_CONTAINER.setWaitStrategy(
        Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofSeconds(60)));
    PG_CONTAINER.start();

    try {
      updateLiquibase();
      RegistryDatabaseInitializer.init(PG_CONTAINER.createConnection(""));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void updateLiquibase() throws SQLException, LiquibaseException {
    Database databaseLiquibase;
    databaseLiquibase =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(
                new JdbcConnection(PG_CONTAINER.createConnection("")));
    Liquibase liquibase =
        new Liquibase(
            TestConstants.LIQUIBASE_MASTER_FILE,
            new ClassLoaderResourceAccessor(),
            databaseLiquibase);
    liquibase.update(new Contexts());
  }
}
