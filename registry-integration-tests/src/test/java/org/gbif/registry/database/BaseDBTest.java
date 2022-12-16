package org.gbif.registry.database;

import org.gbif.registry.ws.it.fixtures.TestConstants;

import java.sql.SQLException;
import java.time.Duration;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class BaseDBTest {

  public static PostgreSQLContainer CONTAINER;
  static {
    CONTAINER = new PostgreSQLContainer("postgres:11.1").withDatabaseName("registry");
    CONTAINER.withReuse(true).withLabel("reuse.tag", "registry_its_pg_container");
    CONTAINER.setWaitStrategy(
      Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofSeconds(60)));
    CONTAINER.start();

    Database databaseLiquibase = null;
    try {
      databaseLiquibase = DatabaseFactory.getInstance()
        .findCorrectDatabaseImplementation(new JdbcConnection(CONTAINER.createConnection("")));
      Liquibase liquibase =
        new Liquibase(TestConstants.LIQUIBASE_MASTER_FILE, new ClassLoaderResourceAccessor(), databaseLiquibase);
      liquibase.update(new Contexts());

//      new RegistryDatabaseInitializer().init(CONTAINER.createConnection(""));
    } catch (DatabaseException e) {
      throw new RuntimeException(e);
    } catch (SQLException | LiquibaseException e) {
      throw new RuntimeException(e);
    }
  }

  @RegisterExtension
  public static RegistryDatabaseInitializer registryDatabaseInitializer = new RegistryDatabaseInitializer(CONTAINER);

}
