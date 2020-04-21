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
package org.gbif.registry.metasync.protocols.tapir;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.util.MachineTagUtils;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.TagName;
import org.gbif.registry.metasync.api.SyncResult;
import org.gbif.registry.metasync.protocols.HttpGetMatcher;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TapirMetadataSynchroniserTest {

  @Mock private HttpClient client;
  private TapirMetadataSynchroniser synchroniser;
  private Installation installation;

  @Before
  public void setup() {
    synchroniser = new TapirMetadataSynchroniser(client);

    installation = new Installation();
    installation.setType(InstallationType.TAPIR_INSTALLATION);
    Endpoint endpoint = new Endpoint();
    endpoint.setUrl(URI.create("http://localhost/nmr"));
    installation.addEndpoint(endpoint);
  }

  @Test
  public void testCanHandle() {
    installation.setType(InstallationType.BIOCASE_INSTALLATION);
    assertThat(synchroniser.canHandle(installation)).isFalse();

    installation.setType(InstallationType.TAPIR_INSTALLATION);
    assertThat(synchroniser.canHandle(installation)).isTrue();
  }

  /** A simple test to see if multiple datasets are parsed successfully. */
  @Test
  public void testAddedDatasets() throws Exception {
    when(client.execute(argThat(HttpGetMatcher.matchUrl("http://localhost/nmr?op=capabilities"))))
        .thenReturn(prepareResponse(200, "tapir/capabilities1.xml"));
    when(client.execute(argThat(HttpGetMatcher.matchUrl("http://localhost/nmr"))))
        .thenReturn(prepareResponse(200, "tapir/metadata1.xml"));
    when(client.execute(
            argThat(
                HttpGetMatcher.matchUrl(
                    "http://localhost/nmr?op=s&t=http%3A%2F%2Frs.gbif.org%2Ftemplates%2Ftapir%2Fdwc%2F1.4%2Fsci_name_range.xml&count=true&start=0&limit=1&lower=AAA&upper=zzz"))))
        .thenReturn(prepareResponse(200, "tapir/search1.xml"));
    SyncResult syncResult = synchroniser.syncInstallation(installation, new ArrayList<Dataset>());
    assertThat(syncResult.deletedDatasets).isEmpty();
    assertThat(syncResult.existingDatasets).isEmpty();
    assertThat(syncResult.addedDatasets).hasSize(1);

    Dataset ds1 = syncResult.addedDatasets.get(0);
    assertThat(ds1.getTitle()).isEqualTo("Natural History Museum Rotterdam");
    assertThat(ds1.getLicense()).isNull();
    assertThat(ds1.getRights()).isNull();
    assertThat(ds1.getContacts()).hasSize(2);
    assertThat(ds1.getMachineTags()).hasSize(2);
    assertThat(ds1.getDoi()).isEqualTo(new DOI("10.1234/doi"));

    // Assert the declared record count machine tag was found, and that its value was 167348
    MachineTag count = MachineTagUtils.firstTag(ds1, TagName.DECLARED_COUNT);
    assertThat(count).isNotNull();
    assertThat(Integer.valueOf(count.getValue())).isEqualTo(167348);
  }

  /**
   * This tests adding a new Dataset that has been assigned a license detected from the rights
   * field.
   */
  @Test
  public void testAddedDatasetsWithLicense() throws Exception {
    when(client.execute(argThat(HttpGetMatcher.matchUrl("http://localhost/nmr?op=capabilities"))))
        .thenReturn(prepareResponse(200, "tapir/capabilities1.xml"));
    when(client.execute(argThat(HttpGetMatcher.matchUrl("http://localhost/nmr"))))
        .thenReturn(prepareResponse(200, "tapir/metadata2.xml"));
    when(client.execute(
            argThat(
                HttpGetMatcher.matchUrl(
                    "http://localhost/nmr?op=s&t=http%3A%2F%2Frs.gbif.org%2Ftemplates%2Ftapir%2Fdwc%2F1.4%2Fsci_name_range.xml&count=true&start=0&limit=1&lower=AAA&upper=zzz"))))
        .thenReturn(prepareResponse(200, "tapir/search1.xml"));
    SyncResult syncResult = synchroniser.syncInstallation(installation, new ArrayList<Dataset>());
    assertThat(syncResult.deletedDatasets).isEmpty();
    assertThat(syncResult.existingDatasets).isEmpty();
    assertThat(syncResult.addedDatasets).hasSize(1);

    Dataset ds1 = syncResult.addedDatasets.get(0);
    assertThat(ds1.getTitle()).isEqualTo("Natural History Museum Rotterdam (2)");
    assertThat(ds1.getLicense()).isEqualTo(License.CC_BY_4_0);
    assertThat(ds1.getRights()).isNull();
    assertThat(ds1.getContacts()).hasSize(2);
    assertThat(ds1.getMachineTags()).hasSize(2);
    assertThat(ds1.getDoi()).isEqualTo(new DOI("10.1234/doi"));
  }

  @Test
  public void testDeletedDataset() throws Exception {
    Dataset dataset = new Dataset();
    dataset.setTitle("Foobar");

    when(client.execute(argThat(HttpGetMatcher.matchUrl("http://localhost/nmr?op=capabilities"))))
        .thenReturn(prepareResponse(200, "tapir/capabilities1.xml"));
    when(client.execute(argThat(HttpGetMatcher.matchUrl("http://localhost/nmr"))))
        .thenReturn(prepareResponse(200, "tapir/metadata1.xml"));
    when(client.execute(
            argThat(
                HttpGetMatcher.matchUrl(
                    "http://localhost/nmr?op=s&t=http%3A%2F%2Frs.gbif.org%2Ftemplates%2Ftapir%2Fdwc%2F1.4%2Fsci_name_range.xml&count=true&start=0&limit=1&lower=AAA&upper=zzz"))))
        .thenReturn(prepareResponse(200, "tapir/search1.xml"));
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
    dataset.setTitle("Foobar");
    Endpoint endpoint = new Endpoint();
    endpoint.setUrl(URI.create("http://localhost/nmr"));
    dataset.addEndpoint(endpoint);

    when(client.execute(argThat(HttpGetMatcher.matchUrl("http://localhost/nmr?op=capabilities"))))
        .thenReturn(prepareResponse(200, "tapir/capabilities1.xml"));
    when(client.execute(argThat(HttpGetMatcher.matchUrl("http://localhost/nmr"))))
        .thenReturn(prepareResponse(200, "tapir/metadata1.xml"));
    when(client.execute(
            argThat(
                HttpGetMatcher.matchUrl(
                    "http://localhost/nmr?op=s&t=http%3A%2F%2Frs.gbif.org%2Ftemplates%2Ftapir%2Fdwc%2F1.4%2Fsci_name_range.xml&count=true&start=0&limit=1&lower=AAA&upper=zzz"))))
        .thenReturn(prepareResponse(200, "tapir/search1.xml"));

    SyncResult syncResult =
        synchroniser.syncInstallation(installation, Lists.newArrayList(dataset));
    assertThat(syncResult.deletedDatasets).describedAs("Deleted datasets").isEmpty();
    assertThat(syncResult.existingDatasets).hasSize(1);
    assertThat(syncResult.addedDatasets).isEmpty();

    assertThat(syncResult.existingDatasets.get(dataset).getTitle())
        .isEqualTo("Natural History Museum Rotterdam");
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
