/*
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
package org.gbif.registry.persistence.mapper;

import java.sql.Connection;
import java.util.UUID;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Testcontainers
public class DatasetValidationMapperTest {

  @Container
  static final PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:14")
          .withDatabaseName("registry")
          .withUsername("registry")
          .withPassword("registry");

  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void setUp() throws Exception {
    org.postgresql.ds.PGSimpleDataSource dataSource = new org.postgresql.ds.PGSimpleDataSource();
    dataSource.setUrl(postgres.getJdbcUrl());
    dataSource.setUser(postgres.getUsername());
    dataSource.setPassword(postgres.getPassword());

    try (Connection connection = dataSource.getConnection()) {
      liquibase.database.Database database = DatabaseFactory.getInstance()
          .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (Liquibase liquibase = new Liquibase(
          "liquibase/master.xml",
          new ClassLoaderResourceAccessor(),
          database)) {
        liquibase.update("");
      }
    }

    Environment environment = new Environment("test", new JdbcTransactionFactory(), dataSource);
    Configuration configuration = new Configuration(environment);
    configuration.addMapper(DatasetValidationMapper.class);
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
  }

  @Test
  void testGetReturnsNullWhenNotFound() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      assertNull(session.getMapper(DatasetValidationMapper.class).get(UUID.randomUUID(), 1));
    }
  }

  @Test
  void testGetLatestReturnsNullWhenNotFound() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      assertNull(session.getMapper(DatasetValidationMapper.class).getLatest(UUID.randomUUID()));
    }
  }

  @Test
  void testCreateOrUpdateAndGet() {
    UUID datasetKey = UUID.randomUUID();

    try (SqlSession session = sqlSessionFactory.openSession()) {
      session.getMapper(DatasetValidationMapper.class).createOrUpdate(datasetKey, 1, "{\"valid\": true}");
      session.commit();
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      assertEquals("{\"valid\": true}", session.getMapper(DatasetValidationMapper.class).get(datasetKey, 1));
    }
  }

  @Test
  void testGetLatestReturnsHighestAttempt() {
    UUID datasetKey = UUID.randomUUID();

    try (SqlSession session = sqlSessionFactory.openSession()) {
      DatasetValidationMapper mapper = session.getMapper(DatasetValidationMapper.class);
      mapper.createOrUpdate(datasetKey, 1, "{\"attempt\": 1}");
      mapper.createOrUpdate(datasetKey, 2, "{\"attempt\": 2}");
      session.commit();
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      assertEquals("{\"attempt\": 2}", session.getMapper(DatasetValidationMapper.class).getLatest(datasetKey));
    }
  }

  @Test
  void testCreateOrUpdateOverwritesExistingAttempt() {
    UUID datasetKey = UUID.randomUUID();

    try (SqlSession session = sqlSessionFactory.openSession()) {
      session.getMapper(DatasetValidationMapper.class).createOrUpdate(datasetKey, 1, "{\"valid\": false}");
      session.commit();
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      session.getMapper(DatasetValidationMapper.class).createOrUpdate(datasetKey, 1, "{\"valid\": true}");
      session.commit();
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      assertEquals("{\"valid\": true}", session.getMapper(DatasetValidationMapper.class).get(datasetKey, 1));
    }
  }

  @Test
  void testGetReturnsCorrectAttempt() {
    UUID datasetKey = UUID.randomUUID();

    try (SqlSession session = sqlSessionFactory.openSession()) {
      DatasetValidationMapper mapper = session.getMapper(DatasetValidationMapper.class);
      mapper.createOrUpdate(datasetKey, 1, "{\"attempt\": 1}");
      mapper.createOrUpdate(datasetKey, 2, "{\"attempt\": 2}");
      session.commit();
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      DatasetValidationMapper mapper = session.getMapper(DatasetValidationMapper.class);
      assertEquals("{\"attempt\": 1}", mapper.get(datasetKey, 1));
      assertEquals("{\"attempt\": 2}", mapper.get(datasetKey, 2));
    }
  }
}
