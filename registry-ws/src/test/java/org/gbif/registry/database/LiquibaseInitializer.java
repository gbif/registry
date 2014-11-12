/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.database;

import java.io.File;
import java.sql.Connection;

import com.zaxxer.hikari.HikariDataSource;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Rule that will run Liquibase to ensure the DB schema is up to date. This is an expensive operation, and unless the
 * application modified the schema during runtime, it should only be run as a {@link ClassRule}. To use this:
 * 
 * <pre>
 * @ClassRule
 * public static LiquibaseInitializer = new LiquibaseInitializer(getDatasource()); // developer required to provide datasource
 * </pre>
 */
public class LiquibaseInitializer extends ExternalResource {

  private static final Logger LOG = LoggerFactory.getLogger(LiquibaseInitializer.class);
  private static final String[] LIQUIBASE_FILES = {"master.xml"};
  private final HikariDataSource dataSource;

  public LiquibaseInitializer(HikariDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  protected void before() throws Throwable {
    LOG.info("Running Liquibase");
    Connection connection = dataSource.getConnection();
    runLiquibase(connection, LIQUIBASE_FILES);
    if (connection != null && !connection.isClosed()) {
      connection.close();
    }
    LOG.info("Liquibase finished successfully");
  }

  protected void runLiquibase(Connection connection, String... fileNames) throws LiquibaseException {
    for (String fileName : fileNames) {
      Liquibase liquibase = new Liquibase("liquibase" + File.separatorChar + fileName,
                                          new ClassLoaderResourceAccessor(),
                                          new JdbcConnection(connection));
      liquibase.forceReleaseLocks(); // often happens when tests are aborted for example
      liquibase.update(null);
    }
  }

  @Override
  protected void after() {
    super.after();
    LOG.info("Shutdown datasource");
    dataSource.close();
  }
}
