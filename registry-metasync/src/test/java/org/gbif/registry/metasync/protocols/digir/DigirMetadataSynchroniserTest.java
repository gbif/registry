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
package org.gbif.registry.metasync.protocols.digir;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.api.vocabulary.TagName;
import org.gbif.registry.metasync.api.SyncResult;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DigirMetadataSynchroniserTest {

  @Mock private HttpClient client;
  private DigirMetadataSynchroniser synchroniser;
  private Installation installation;

  @Before
  public void setup() {
    synchroniser = new DigirMetadataSynchroniser(client);

    installation = new Installation();
    installation.setType(InstallationType.DIGIR_INSTALLATION);
    Endpoint endpoint = new Endpoint();
    endpoint.setUrl(URI.create("http://localhost"));
    installation.addEndpoint(endpoint);
  }

  @Test
  public void testCanHandle() {
    installation.setType(InstallationType.BIOCASE_INSTALLATION);
    assertThat(synchroniser.canHandle(installation)).isFalse();

    installation.setType(InstallationType.DIGIR_INSTALLATION);
    assertThat(synchroniser.canHandle(installation)).isTrue();
  }

  /** A simple test to see if multiple datasets are parsed successfully. */
  @Test
  public void testAddedDatasets() throws Exception {
    when(client.execute(any(HttpGet.class))).thenReturn(prepareResponse(200, "digir/test1.xml"));
    SyncResult syncResult = synchroniser.syncInstallation(installation, new ArrayList<Dataset>());
    assertThat(syncResult.deletedDatasets).isEmpty();
    assertThat(syncResult.existingDatasets).isEmpty();
    assertThat(syncResult.addedDatasets).hasSize(8);
  }

  /**
   * This tests a Metadata response with just one resources but validates more things from this
   * parsed Dataset.
   */
  @Test
  public void testAddedDataset() throws Exception {
    when(client.execute(any(HttpGet.class))).thenReturn(prepareResponse(200, "digir/test2.xml"));
    SyncResult syncResult = synchroniser.syncInstallation(installation, new ArrayList<Dataset>());
    assertThat(syncResult.deletedDatasets).isEmpty();
    assertThat(syncResult.existingDatasets).isEmpty();
    assertThat(syncResult.addedDatasets).hasSize(1);

    Dataset dataset = syncResult.addedDatasets.get(0);
    assertThat(dataset.getTitle())
        .isEqualTo("Distribution of benthic foraminifera of sediment core PS1388-3");
    assertThat(dataset.getHomepage())
        .isEqualTo(URI.create("http://doi.pangaea.de/doi:10.1594/PANGAEA.51131"));
    assertThat(dataset.getCitation().getText())
        .isEqualTo(
            "Mackensen, Andreas; Grobe, Hannes; Hubberten, Hans-Wolfgang; Spieß, Volkhard; Fütterer, Dieter K (1989): Distribution of benthic foraminifera of sediment core PS1388-3, doi:10.1594/PANGAEA.51131");
    assertThat(dataset.getIdentifiers().size())
        .isEqualTo(0); // verify that we don't create an identifier for the DOI
    assertThat(dataset.getDoi().getDoiName()).isEqualTo("10.1594/pangaea.51131");
    // endpoints
    assertThat(dataset.getEndpoints().size()).isEqualTo(1);
    assertThat(dataset.getEndpoints().get(0).getType()).isEqualTo(EndpointType.DIGIR);
  }

  @Test
  public void testDeletedDataset() throws Exception {
    Dataset dataset = new Dataset();
    dataset.addMachineTag(MachineTag.newInstance(TagName.DIGIR_CODE, "foobar"));
    dataset.setTitle("Foobar");

    when(client.execute(any(HttpGet.class))).thenReturn(prepareResponse(200, "digir/test2.xml"));
    SyncResult syncResult =
        synchroniser.syncInstallation(installation, Lists.newArrayList(dataset));
    assertThat(syncResult.deletedDatasets).hasSize(1);
    assertThat(syncResult.existingDatasets).isEmpty();
    assertThat(syncResult.addedDatasets).hasSize(1);

    assertThat(syncResult.deletedDatasets.get(0).getTitle()).isEqualTo("Foobar");
  }

  @Test
  public void testUpdatedDataset() throws Exception {
    Dataset dataset = new Dataset();
    dataset.addMachineTag(MachineTag.newInstance(TagName.DIGIR_CODE, "doi:10.1594/PANGAEA.51131"));
    dataset.setTitle("Foobar");

    when(client.execute(any(HttpGet.class))).thenReturn(prepareResponse(200, "digir/test2.xml"));
    SyncResult syncResult =
        synchroniser.syncInstallation(installation, Lists.newArrayList(dataset));
    assertThat(syncResult.deletedDatasets).describedAs("Deleted datasets").isEmpty();
    assertThat(syncResult.existingDatasets).hasSize(1);
    assertThat(syncResult.addedDatasets).isEmpty();

    assertThat(syncResult.existingDatasets.get(dataset).getTitle())
        .isEqualTo("Distribution of benthic foraminifera of sediment core PS1388-3");
  }

  /** Make sure the determination of DiGIR endpoint type is catching DIGIR_MANIS. */
  @Test
  public void testDetermineEndpointType() {
    // populate map with namespace (conceptualSchema) / schemaLocation key value pair
    Map<String, URI> schemas = Maps.newHashMap();
    schemas.put(
        "http://digir.net/schema/conceptual/darwin/2003/1.0",
        URI.create("http://bnhm.berkeley.museum/manis/DwC/darwin2jrw030315.xsd"));
    assertThat(synchroniser.determineEndpointType(schemas)).isEqualTo(EndpointType.DIGIR_MANIS);
    // reset, try another
    schemas.clear();
    schemas.put(
        "http://digir.net/schema/conceptual/darwin/2003/1.0",
        URI.create("http://bnhm.berkeley.edu/DwC/bnhm_dc2_schema.xsd"));
    assertThat(synchroniser.determineEndpointType(schemas)).isEqualTo(EndpointType.DIGIR_MANIS);
  }

  public HttpResponse prepareResponse(int responseStatus, String fileName) throws IOException {
    HttpResponse response =
        new BasicHttpResponse(
            new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), responseStatus, ""));
    response.setStatusCode(responseStatus);
    byte[] bytes = Resources.toByteArray(Resources.getResource(fileName));
    response.setEntity(new ByteArrayEntity(bytes));
    return response;
  }
}
