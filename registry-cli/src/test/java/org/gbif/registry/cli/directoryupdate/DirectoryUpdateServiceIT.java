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
package org.gbif.registry.cli.directoryupdate;

import org.gbif.api.model.registry.Node;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.cli.util.RegistryCliUtils;
import org.gbif.registry.persistence.mapper.NodeMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;

import static org.gbif.registry.cli.util.EmbeddedPostgresTestUtils.LIQUIBASE_MASTER_FILE;
import static org.gbif.registry.cli.util.EmbeddedPostgresTestUtils.toDbConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Test Registry updates from the Directory */
@SuppressWarnings("UnstableApiUsage")
public class DirectoryUpdateServiceIT {

  private static final UUID TOGO_NODE_UUID =
      UUID.fromString("c9659a3e-07e9-4fcb-83c6-de8b9009a02e");

  private static DirectoryUpdateConfiguration directoryUpdateConfig;
  private static Connection registryDbConnection;

  @RegisterExtension
  public static PreparedDbExtension database =
      EmbeddedPostgresExtension.preparedDatabase(
          LiquibasePreparer.forClasspathLocation(LIQUIBASE_MASTER_FILE));

  @BeforeAll
  public static void beforeAll() throws Exception {
    directoryUpdateConfig =
        RegistryCliUtils.loadConfig(
            "directoryupdate/directory-update.yaml", DirectoryUpdateConfiguration.class);
    directoryUpdateConfig.db = toDbConfig(database);

    registryDbConnection = database.getTestDatabase().getConnection();
  }

  @AfterAll
  public static void afterAll() throws Exception {
    registryDbConnection.close();
  }

  @BeforeEach
  public void before() throws Exception {
    String registrySql = RegistryCliUtils.getFileData("directoryupdate/prepare_registry.sql");
    PreparedStatement registryPS = registryDbConnection.prepareStatement(registrySql);
    registryPS.executeUpdate();
  }

  @AfterEach
  public void after() throws Exception {
    String registrySql = RegistryCliUtils.getFileData("directoryupdate/clean_registry.sql");
    PreparedStatement registryPS = registryDbConnection.prepareStatement(registrySql);
    registryPS.executeUpdate();
  }

  @Test
  public void testDirectoryUpdateAndCreate() {
    DirectoryUpdateService directoryUpdateService =
        new DirectoryUpdateService(directoryUpdateConfig);
    DirectoryUpdater directoryUpdater =
        directoryUpdateService.getContext().getBean(DirectoryUpdater.class);

    directoryUpdater.applyUpdates();

    NodeMapper nodeMapper = directoryUpdateService.getContext().getBean(NodeMapper.class);
    int nodesCount = nodeMapper.count();
    org.gbif.api.model.registry.Node togoNode = nodeMapper.get(TOGO_NODE_UUID);
    Node ugandaNode = nodeMapper.getByCountry(Country.UGANDA);

    assertNotEquals(
        2,
        nodesCount,
        "After updating from the Directory total amount of nodes must be more than initial 2");
    assertEquals(
        "GBIF Uganda",
        ugandaNode.getTitle(),
        "The Uganda node is created with the name of the Node from the Directory");
    assertEquals(
        "GBIF Togo",
        togoNode.getTitle(),
        "The Togo node is updated with the name of the Node from the Directory");
  }
}
