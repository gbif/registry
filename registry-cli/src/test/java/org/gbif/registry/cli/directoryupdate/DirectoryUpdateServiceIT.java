/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
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
package org.gbif.registry.cli.directoryupdate;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Node;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.cli.util.RegistryCliUtils;
import org.gbif.registry.persistence.mapper.NodeMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

/** Test Registry updates from the Directory */
@SuppressWarnings("UnstableApiUsage")
@Ignore
public class DirectoryUpdateServiceIT {

  private static final UUID TOGO_NODE_UUID =
      UUID.fromString("0e655f55-8a9d-498f-9903-33058b78d006");

  private static DirectoryUpdateConfiguration directoryUpdateConfig;
  private static Connection registryDbConnection;

  @BeforeClass
  public static void beforeClass() throws Exception {
    directoryUpdateConfig =
        RegistryCliUtils.loadConfig(
            "directoryupdate/directory-update.yaml", DirectoryUpdateConfiguration.class);
    registryDbConnection =
        RegistryCliUtils.prepareConnection(
            directoryUpdateConfig.db.serverName,
            directoryUpdateConfig.db.databaseName,
            directoryUpdateConfig.db.user,
            directoryUpdateConfig.db.password);
  }

  @AfterClass
  public static void afterClass() throws Exception {
    registryDbConnection.close();
  }

  @Before
  public void before() throws Exception {
    String registrySql = RegistryCliUtils.getFileData("directoryupdate/prepare_registry.sql");
    PreparedStatement registryPS = registryDbConnection.prepareStatement(registrySql);
    registryPS.executeUpdate();
  }

  @After
  public void after() throws Exception {
    String registrySql = RegistryCliUtils.getFileData("directoryupdate/clean_registry.sql");
    PreparedStatement registryPS = registryDbConnection.prepareStatement(registrySql);
    registryPS.executeUpdate();
  }

  @Test
  public void testDirectoryUpdateAndCreate() {
    DirectoryUpdateService directoryUpdateService =
        new DirectoryUpdateService(directoryUpdateConfig);

    directoryUpdateService.startAsync();

    // maybe a little bit weak
    // TODO: 20/03/2020 async service, use awaitility?
    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail();
    }

    directoryUpdateService.stopAsync();
    try {
      directoryUpdateService.awaitTerminated(1, TimeUnit.MINUTES);
    } catch (TimeoutException e) {
      e.printStackTrace();
      fail();
    }

    NodeMapper nodeMapper = directoryUpdateService.getContext().getBean(NodeMapper.class);
    List<org.gbif.api.model.registry.Node> registryNodes =
        nodeMapper.list(new PagingRequest(0, 1000));
    org.gbif.api.model.registry.Node togoNode = nodeMapper.get(TOGO_NODE_UUID);
    Node ugandaNode = nodeMapper.getByCountry(Country.UGANDA);

    assertNotEquals(
        "After updating from the Directory total amount of nodes must be more than initial 2",
        2,
        registryNodes.size());
    assertEquals(
        "The Uganda node is created with the name of the Node from the Directory",
        "GBIF Uganda",
        ugandaNode.getTitle());
    assertEquals(
        "The Togo node is updated with the name of the Node from the Directory",
        "GBIF Togo",
        togoNode.getTitle());
  }
}
