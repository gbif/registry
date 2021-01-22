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
package org.gbif.registry.metasync.protocols.biocase;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.metasync.api.SyncResult;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BiocaseMetadataSynchroniserTest {

  @Mock private HttpClient client;
  private BiocaseMetadataSynchroniser synchroniser;
  private Installation installation;

  @BeforeEach
  public void setup() {
    synchroniser = new BiocaseMetadataSynchroniser(client);
    installation = new Installation();
    installation.setKey(UUID.randomUUID());
    installation.setType(InstallationType.BIOCASE_INSTALLATION);
    Endpoint endpoint = new Endpoint();
    endpoint.setKey(1);
    endpoint.setUrl(URI.create("http://localhost"));
    installation.addEndpoint(endpoint);
  }

  @Test
  public void testCanHandle() {
    installation.setType(InstallationType.DIGIR_INSTALLATION);
    assertFalse(synchroniser.canHandle(installation));

    installation.setType(InstallationType.BIOCASE_INSTALLATION);
    assertTrue(synchroniser.canHandle(installation));
  }

  /** This tests a BioCASe endpoint that supports the old style inventory and ABCD 2.06 */
  @Test
  public void testAddedDataset1() throws Exception {
    when(client.execute(any(HttpGet.class)))
        .thenReturn(prepareResponse(200, "biocase/capabilities1.xml"))
        .thenReturn(prepareResponse(200, "biocase/inventory1.xml"))
        .thenReturn(prepareResponse(200, "biocase/dataset1.xml"));
    SyncResult syncResult = synchroniser.syncInstallation(installation, new ArrayList<>());
    assertNull(syncResult.exception);
    assertTrue(syncResult.deletedDatasets.isEmpty());
    assertTrue(syncResult.existingDatasets.isEmpty());
    assertEquals(1, syncResult.addedDatasets.size());

    Dataset dataset = syncResult.addedDatasets.get(0);
    assertEquals("Pontaurus", dataset.getTitle());
    assertNotNull(dataset.getCitation());
    assertEquals("All credit to Markus Doring", dataset.getCitation().getText());
    assertNull(dataset.getDoi());

    // License set to UNSPECIFIED because no machine readable license detected in metadata
    // Note: all new datasets without a license get assigned default license (CC-BY 4.0) when
    // registered/persisted
    assertEquals(License.UNSPECIFIED, dataset.getLicense());
    assertNull(dataset.getRights());

    // endpoints
    assertEquals(1, dataset.getEndpoints().size());
    assertEquals(EndpointType.BIOCASE, dataset.getEndpoints().get(0).getType());
  }

  /** This tests a BioCASe endpoint that supports the new style inventory and ABCD 2.06 */
  @Test
  public void testAddedDataset2() throws Exception {
    when(client.execute(any(HttpGet.class)))
        .thenReturn(prepareResponse(200, "biocase/capabilities2.xml"))
        .thenReturn(prepareResponse(200, "biocase/inventory2.xml"))
        .thenReturn(prepareResponse(200, "biocase/dataset2.xml"));
    SyncResult syncResult = synchroniser.syncInstallation(installation, new ArrayList<>());
    assertNull(syncResult.exception);
    assertTrue(syncResult.deletedDatasets.isEmpty());
    assertTrue(syncResult.existingDatasets.isEmpty());
    assertEquals(1, syncResult.addedDatasets.size());

    Dataset dataset = syncResult.addedDatasets.get(0);
    assertEquals(new DOI("10.1234/doi"), dataset.getDoi());
    assertEquals("Collections of Phytoplankton at BGBM", dataset.getTitle());
    assertNotNull(dataset.getCitation());
    assertEquals("Jahn, R. (Ed.) 2013+ (continuously updated): Collections of Phytoplankton at BGBM",
        dataset.getCitation().getText());

    // endpoints
    assertEquals(3, dataset.getEndpoints().size());
    assertEquals(EndpointType.DWC_ARCHIVE, dataset.getEndpoints().get(0).getType());
    assertEquals(EndpointType.BIOCASE_XML_ARCHIVE, dataset.getEndpoints().get(1).getType());
    assertEquals(EndpointType.BIOCASE, dataset.getEndpoints().get(2).getType());
  }

  /** This tests a BioCASe endpoint that supports the old style inventory and ABCD 1.2 */
  @Test
  public void testAddedDataset3() throws Exception {
    when(client.execute(any(HttpGet.class)))
        .thenReturn(prepareResponse(200, "biocase/capabilities3.xml"))
        .thenReturn(prepareResponse(200, "biocase/inventory3.xml"))
        .thenReturn(prepareResponse(200, "biocase/dataset3.xml"));
    SyncResult syncResult = synchroniser.syncInstallation(installation, new ArrayList<>());
    assertNull(syncResult.exception);
    assertTrue(syncResult.deletedDatasets.isEmpty());
    assertTrue(syncResult.existingDatasets.isEmpty());
    assertEquals(4, syncResult.addedDatasets.size());

    Dataset dataset = syncResult.addedDatasets.get(0);
    assertNull(dataset.getDoi());
    assertEquals("Mammals housed at MHNG, Geneva", dataset.getTitle());
    assertNotNull(dataset.getCitation());
    assertEquals("Ruedi M. Mammals housed at MHNG, Geneva. Muséum d'histoire naturelle de la Ville de Genève",
        dataset.getCitation().getText());

    // endpoints
    assertEquals(1, dataset.getEndpoints().size());
    assertEquals(EndpointType.BIOCASE, dataset.getEndpoints().get(0).getType());
  }

  /**
   * This tests a BioCASe endpoint that supports the old style inventory and ABCD 2.06 and adds a
   * new dataset that has been assigned a license using License/Text.
   */
  @Test
  public void testAddedDataset4() throws Exception {
    when(client.execute(any(HttpGet.class)))
        .thenReturn(prepareResponse(200, "biocase/capabilities1.xml"))
        .thenReturn(prepareResponse(200, "biocase/inventory1.xml"))
        .thenReturn(prepareResponse(200, "biocase/dataset4.xml"));
    SyncResult syncResult = synchroniser.syncInstallation(installation, new ArrayList<>());
    assertNull(syncResult.exception);
    assertTrue(syncResult.deletedDatasets.isEmpty());
    assertTrue(syncResult.existingDatasets.isEmpty());
    assertEquals(1, syncResult.addedDatasets.size());

    Dataset dataset = syncResult.addedDatasets.get(0);
    assertEquals("Pontaurus", dataset.getTitle());
    // dectected license CC0 1.0 in License/Text="CC0"
    assertEquals(License.CC0_1_0, dataset.getLicense());
    assertNull(dataset.getRights());
    // endpoints
    assertEquals(1, dataset.getEndpoints().size());
    assertEquals(EndpointType.BIOCASE, dataset.getEndpoints().get(0).getType());
  }

  /**
   * This tests a BioCASe endpoint that supports the old style inventory and ABCD 2.06 and adds a
   * new dataset that has been assigned a license using License/URI (using https!).
   */
  @Test
  public void testAddedDataset5() throws Exception {
    when(client.execute(any(HttpGet.class)))
        .thenReturn(prepareResponse(200, "biocase/capabilities1.xml"))
        .thenReturn(prepareResponse(200, "biocase/inventory1.xml"))
        .thenReturn(prepareResponse(200, "biocase/dataset5.xml"));
    SyncResult syncResult = synchroniser.syncInstallation(installation, new ArrayList<>());
    assertNull(syncResult.exception);
    assertTrue(syncResult.deletedDatasets.isEmpty());
    assertTrue(syncResult.existingDatasets.isEmpty());
    assertEquals(1, syncResult.addedDatasets.size());

    Dataset dataset = syncResult.addedDatasets.get(0);
    assertEquals("Pontaurus", dataset.getTitle());
    // detected license CC0 1.0 in
    // License/URI="https://creativecommons.org/publicdomain/zero/1.0/legalcode"
    assertEquals(License.CC0_1_0, dataset.getLicense());
    assertNull(dataset.getRights());
    // endpoints
    assertEquals(1, dataset.getEndpoints().size());
    assertEquals(EndpointType.BIOCASE, dataset.getEndpoints().get(0).getType());
  }

  /**
   * This tests a BioCASe endpoint that supports the old style inventory and ABCD 2.06 and adds a
   * new dataset that has been assigned a license detected from the rights field.
   */
  @Test
  public void testAddedDataset6() throws Exception {
    when(client.execute(any(HttpGet.class)))
        .thenReturn(prepareResponse(200, "biocase/capabilities1.xml"))
        .thenReturn(prepareResponse(200, "biocase/inventory1.xml"))
        .thenReturn(prepareResponse(200, "biocase/dataset6.xml"));
    SyncResult syncResult = synchroniser.syncInstallation(installation, new ArrayList<>());
    assertNull(syncResult.exception);
    assertTrue(syncResult.deletedDatasets.isEmpty());
    assertTrue(syncResult.existingDatasets.isEmpty());
    assertEquals(1, syncResult.addedDatasets.size());

    Dataset dataset = syncResult.addedDatasets.get(0);
    assertEquals("Pontaurus", dataset.getTitle());
    // dectected license CC-BY 4.0 in rights="CC-BY"
    assertEquals(License.CC_BY_4_0, dataset.getLicense());
    assertNull(dataset.getRights());
    // endpoints
    assertEquals(1, dataset.getEndpoints().size());
    assertEquals(EndpointType.BIOCASE, dataset.getEndpoints().get(0).getType());
  }

  @Test
  public void testDeletedDataset() throws Exception {
    Dataset dataset = new Dataset();
    dataset.setTitle("Foobar");

    when(client.execute(any(HttpGet.class)))
        .thenReturn(prepareResponse(200, "biocase/capabilities1.xml"))
        .thenReturn(prepareResponse(200, "biocase/inventory1.xml"))
        .thenReturn(prepareResponse(200, "biocase/dataset1.xml"));
    SyncResult syncResult =
        synchroniser.syncInstallation(installation, Lists.newArrayList(dataset));
    assertEquals(1, syncResult.deletedDatasets.size());
    assertTrue(syncResult.existingDatasets.isEmpty());
    assertEquals(1, syncResult.addedDatasets.size());

    assertEquals("Foobar", syncResult.deletedDatasets.get(0).getTitle());
  }

  @Test
  public void testUpdatedDataset() throws Exception {
    Dataset dataset = new Dataset();
    dataset.setTitle("Pontaurus");

    when(client.execute(any(HttpGet.class)))
        .thenReturn(prepareResponse(200, "biocase/capabilities1.xml"))
        .thenReturn(prepareResponse(200, "biocase/inventory1.xml"))
        .thenReturn(prepareResponse(200, "biocase/dataset1.xml"));
    SyncResult syncResult =
        synchroniser.syncInstallation(installation, Lists.newArrayList(dataset));

    assertTrue(syncResult.deletedDatasets.isEmpty());
    assertEquals(1, syncResult.existingDatasets.size());
    assertTrue(syncResult.addedDatasets.isEmpty());

    assertEquals("Pontaurus", syncResult.existingDatasets.get(dataset).getTitle());
  }

  /** Prepares a {@link HttpResponse} with the given response status and the content of the file. */
  @SuppressWarnings("UnstableApiUsage")
  private HttpResponse prepareResponse(int responseStatus, String fileName) throws IOException {
    HttpResponse response =
        new BasicHttpResponse(
            new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), responseStatus, ""));
    response.setStatusCode(responseStatus);
    byte[] bytes = Resources.toByteArray(Resources.getResource(fileName));
    response.setEntity(new ByteArrayEntity(bytes));
    return response;
  }
}
