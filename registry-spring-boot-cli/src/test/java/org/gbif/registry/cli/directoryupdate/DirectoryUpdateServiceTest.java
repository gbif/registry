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

import org.gbif.api.model.directory.Node;
import org.gbif.api.model.directory.Participant;
import org.gbif.api.service.directory.ParticipantService;
import org.gbif.registry.cli.directoryupdate.mapper.RegistryIdentifierMockMapper;
import org.gbif.registry.cli.directoryupdate.mapper.RegistryNodeMockMapper;
import org.gbif.registry.cli.directoryupdate.service.NodeServiceMock;
import org.gbif.registry.cli.directoryupdate.service.ParticipantServiceMock;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.NodeMapper;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import com.beust.jcommander.internal.Lists;
import com.google.inject.Injector;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

/** Test Registry updates from the Directory */
public class DirectoryUpdateServiceTest {

  @Test
  public void testDirectoryUpdate() {

    Injector mockInjector = DirectoryUpdateTestProvider.getMockInjector();

    DirectoryUpdateService serviceUnderTest =
        new DirectoryUpdateService(new DirectoryUpdateConfiguration(), null); // TODO: mockInjector

    RegistryNodeMockMapper nodeMapper =
        (RegistryNodeMockMapper) mockInjector.getInstance(NodeMapper.class);
    ParticipantServiceMock directoryParticipantService =
        (ParticipantServiceMock) mockInjector.getInstance(ParticipantService.class);
    NodeServiceMock directoryNodeService =
        (NodeServiceMock)
            mockInjector.getInstance(org.gbif.api.service.directory.NodeService.class);

    // generate and set mock data
    List<Participant> mockParticipants =
        Lists.newArrayList(
            MockDataGenerator.generateDirectoryParticipant("P 1", 1),
            MockDataGenerator.generateDirectoryParticipant("P 2", 2));
    List<Node> mockNodes = Lists.newArrayList(MockDataGenerator.generateDirectoryNode("N 1", 1));
    directoryParticipantService.setParticipants(mockParticipants);
    directoryNodeService.setNodes(mockNodes);

    List<org.gbif.api.model.registry.Node> mockRegistryNodes =
        Lists.newArrayList(
            MockDataGenerator.generateRegistryNode("RN-1", 1),
            MockDataGenerator.generateRegistryNode("RN-2", 2),
            MockDataGenerator.generateRegistryNode("RN-3", null));
    nodeMapper.setNodes(mockRegistryNodes);

    serviceUnderTest.startAsync();

    // maybe a little bit weak
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail();
    }

    serviceUnderTest.stopAsync();
    try {
      serviceUnderTest.awaitTerminated(1, TimeUnit.MINUTES);
    } catch (TimeoutException e) {
      e.printStackTrace();
      fail();
    }

    List<org.gbif.api.model.registry.Node> updatedRegistryNodes = nodeMapper.getUpdatedNodes();
    assertEquals(2, updatedRegistryNodes.size());
    assertEquals(
        "The title is updated with the name of the Node from the Directory",
        "N 1",
        updatedRegistryNodes.get(0).getTitle());
    assertEquals(
        "The title is updated with the name of the Participant from the Directory (no Directory node exists)",
        "P 2",
        updatedRegistryNodes.get(1).getTitle());
  }

  @Test
  public void testDirectoryCreate() {

    Injector mockInjector = DirectoryUpdateTestProvider.getMockInjector();
    DirectoryUpdateService serviceUnderTest =
        new DirectoryUpdateService(new DirectoryUpdateConfiguration(), null); // TODO: mockInjector

    RegistryNodeMockMapper nodeMapper =
        (RegistryNodeMockMapper) mockInjector.getInstance(NodeMapper.class);
    RegistryIdentifierMockMapper identifierMapper =
        (RegistryIdentifierMockMapper) mockInjector.getInstance(IdentifierMapper.class);

    ParticipantServiceMock directoryParticipantService =
        (ParticipantServiceMock) mockInjector.getInstance(ParticipantService.class);
    NodeServiceMock directoryNodeService =
        (NodeServiceMock)
            mockInjector.getInstance(org.gbif.api.service.directory.NodeService.class);

    // generate and set mock data
    List<Participant> mockParticipants =
        Lists.newArrayList(
            MockDataGenerator.generateDirectoryParticipant("P 1", 1),
            MockDataGenerator.generateDirectoryParticipant("P 2", 2));
    List<Node> mockNodes = Lists.newArrayList(MockDataGenerator.generateDirectoryNode("N 1", 1));
    directoryParticipantService.setParticipants(mockParticipants);
    directoryNodeService.setNodes(mockNodes);

    serviceUnderTest.startAsync();

    // maybe a little bit weak
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail();
    }

    serviceUnderTest.stopAsync();
    try {
      serviceUnderTest.awaitTerminated(1, TimeUnit.MINUTES);
    } catch (TimeoutException e) {
      e.printStackTrace();
      fail();
    }

    List<org.gbif.api.model.registry.Node> createdRegistryNodes = nodeMapper.getCreatedNodes();
    assertEquals(2, createdRegistryNodes.size());
    assertEquals(2, identifierMapper.getCreated().size());
    assertEquals(
        "The Registry Node is created from the Directory",
        "N 1",
        createdRegistryNodes.get(0).getTitle());
  }
}
