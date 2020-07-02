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
package org.gbif;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.api.vocabulary.InstallationType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import static org.gbif.config.RegistryWsClientFactory.datasetService;
import static org.gbif.config.RegistryWsClientFactory.datasetServiceReadOnly;
import static org.gbif.config.RegistryWsClientFactory.installationService;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Ignore("Requires live UAT API")
public class RegistryWsIT {
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(RegistryWsIT.class);
  private static final String DATASET_TITLE = "Dataset - Ws Client Demo";

  private static UUID datasetKey;
  private static UUID installationKey;

  /**
   * Create Dataset one time only. Since nodes and organizations can't be created using the API, an
   * existing organization (endorsed already) is used as both the publishing organization for the
   * dataset, and the hosting organization for the installation.
   */
  @BeforeClass
  public static void createDataset() {
    // Publishing and hosting organization: Test Organization #1, endorsed by DanBIF
    UUID organizationKey = UUID.fromString("0a16da09-7719-40de-8d4f-56a15ed52fb6");

    // create new installation, indicating which organization is hosting the installation
    Installation installation = new Installation();
    installation.setTitle("Installation - Ws Client Demo");
    installation.setType(InstallationType.IPT_INSTALLATION);
    installation.setDisabled(false);
    installation.setDescription("Test IPT instance created using GBIF Registry ws client");
    installation.setOrganizationKey(organizationKey);
    installationKey = installationService().create(installation);

    // create new dataset, indicating which organization owns the dataset, and which installation
    // hosts the dataset
    Dataset dataset = new Dataset();
    dataset.setTitle(DATASET_TITLE);
    dataset.setType(DatasetType.OCCURRENCE);
    dataset.setDescription("Test Dataset created using GBIF Registry ws client");
    dataset.setPublishingOrganizationKey(organizationKey);
    dataset.setInstallationKey(installationKey);
    datasetKey = datasetService().create(dataset);
    LOG.info("Dataset created with key: {}", datasetKey);
  }

  /** Read dataset */
  @Test
  public void testReadDataset() {
    Dataset dataset = datasetServiceReadOnly().get(datasetKey);
    assertEquals(DATASET_TITLE, dataset.getTitle());
  }

  /** Update dataset, add an enpoint. */
  @Test
  public void testAddEndpoint() throws URISyntaxException {
    Endpoint endpoint = new Endpoint();
    endpoint.setUrl(new URI("http://ipt.gbif.org/archive.do?r=diptera"));
    endpoint.setType(EndpointType.DWC_ARCHIVE);
    datasetService().addEndpoint(datasetKey, endpoint);
  }

  /** Update dataset: add a contact. */
  @Test
  public void testAddContact() {
    Contact contact = new Contact();
    contact.setFirstName("Jose");
    contact.setLastName("Cuadra");
    contact.setEmail(Lists.newArrayList("jcuadra@abc.org"));
    contact.setType(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT);
    datasetService().addContact(datasetKey, contact);
  }

  /** Delete dataset, still exists but deleted flag has been set. */
  @Test
  public void testDeleteDataset() {
    datasetService().delete(datasetKey);
    Dataset dataset = datasetServiceReadOnly().get(datasetKey);
    assertNotNull(dataset.getDeleted());
  }

  /** Delete installation, still exists but deleted flag has been set. */
  @Test
  public void testDeleteInstallation() {
    installationService().delete(installationKey);
    Installation installation = installationService().get(installationKey);
    assertNotNull(installation.getDeleted());
  }

  /**
   * Retrieve dataset Pontaurus with key "b3e760d8-8dcc-468c-a23d-8e9772affe59". It is assumed
   * dataset Pontaurus will always exist in every Registry database.
   */
  @Test
  public void testGetDataset() {
    DatasetService ds = datasetServiceReadOnly();
    Dataset dataset = ds.get(UUID.fromString("8575f23e-f762-11e1-a439-00145eb45e9a"));
    assertEquals("PonTaurus collection", dataset.getTitle());
  }

  /** Update dataset user has permission to edit. */
  @Test
  public void testUpdateDataset() {
    DatasetService ds = datasetService();
    Dataset dataset = ds.get(UUID.fromString("38b0f0f5-cac1-48e6-be85-b9f8cdd5ca93"));
    dataset.setRights("CC0");
    ds.update(dataset);
  }
}
