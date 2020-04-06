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
package org.gbif.registry.oaipmh;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.utils.file.FileUtils;

import java.net.URI;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.serviceprovider.exceptions.CannotDisseminateFormatException;
import org.dspace.xoai.serviceprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.serviceprovider.parameters.GetRecordParameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the OAI-PMH endpoint using the XOAI OAI-PMH client library. Test the GetRecord verb of the
 * OAI-PMH endpoint.
 */
@RunWith(Parameterized.class)
public class OaipmhGetRecordIT extends AbstractOaipmhEndpointIT {

  public OaipmhGetRecordIT(
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService,
      DatasetService datasetService,
      TestDataFactory testDataFactory) {
    super(nodeService, organizationService, installationService, datasetService, testDataFactory);
  }

  @Test(expected = IdDoesNotExistException.class)
  public void getRecordNotFound() throws Throwable {

    serviceProvider.getRecord(
        GetRecordParameters.request()
            .withIdentifier("non-existent-record-identifier")
            .withMetadataFormatPrefix(EML_FORMAT.getMetadataPrefix()));
  }

  @Test(expected = CannotDisseminateFormatException.class)
  public void getRecordUnsupportedMetadataFormat() throws Exception {
    Organization org1 = createOrganization(Country.UNITED_KINGDOM);
    Installation org1Installation1 = createInstallation(org1.getKey());
    Dataset org1Installation1Dataset1 =
        createDataset(org1.getKey(), org1Installation1.getKey(), DatasetType.CHECKLIST, new Date());

    String key = org1Installation1Dataset1.getKey().toString();

    serviceProvider.getRecord(
        GetRecordParameters.request()
            .withIdentifier(key)
            .withMetadataFormatPrefix("made-up-metadata-format"));
  }

  @Test
  public void getRecordFound() throws Exception {
    Organization org1 = createOrganization(Country.ZAMBIA);
    Installation org1Installation1 = createInstallation(org1.getKey());
    Dataset org1Installation1Dataset1 =
        createDataset(org1.getKey(), org1Installation1.getKey(), DatasetType.CHECKLIST, new Date());

    String key = org1Installation1Dataset1.getKey().toString();

    Record record =
        serviceProvider.getRecord(
            GetRecordParameters.request()
                .withIdentifier(key)
                .withMetadataFormatPrefix(EML_FORMAT.getMetadataPrefix()));
    assertEquals(key, record.getHeader().getIdentifier());
  }

  @Test
  public void getRecordWithAugmentedMetadata() throws Exception {
    Organization org1 = createOrganization(Country.ZAMBIA);
    Installation org1Installation1 = createInstallation(org1.getKey());
    Dataset org1Installation1Dataset1 =
        createDataset(org1.getKey(), org1Installation1.getKey(), DatasetType.CHECKLIST, new Date());

    insertMetadata(
        org1Installation1Dataset1.getKey(), FileUtils.classpathStream("metadata/sample.xml"));

    String key = org1Installation1Dataset1.getKey().toString();

    Record record =
        serviceProvider.getRecord(
            GetRecordParameters.request()
                .withIdentifier(key)
                .withMetadataFormatPrefix(EML_FORMAT.getMetadataPrefix()));
    assertEquals(key, record.getHeader().getIdentifier());

    // the xoai library doesn't return the <metadata> content as we expect so test the returned
    // document directly.
    String result =
        IOUtils.toString(
            new URI(
                    baseUrl
                        + "?verb=GetRecord&metadataPrefix="
                        + EML_FORMAT.getMetadataPrefix()
                        + "&identifier="
                        + record.getHeader().getIdentifier().toString())
                .toURL(),
            "UTF-8");

    // ensure we get the augmented metadata data
    assertTrue(
        "GetRecord verb returns augmented metadata",
        result.contains(
            "<citation identifier=\"doi:tims-ident.2136.ex43.33.d\">title 1</citation>"));
  }

  @Test
  public void testGetDeletedRestoredRecord() throws Exception {
    Organization org1 = createOrganization(Country.ZAMBIA);
    Installation org1Installation1 = createInstallation(org1.getKey());
    Dataset org1Installation1Dataset1 =
        createDataset(org1.getKey(), org1Installation1.getKey(), DatasetType.CHECKLIST, new Date());

    String key = org1Installation1Dataset1.getKey().toString();

    Record record =
        serviceProvider.getRecord(
            GetRecordParameters.request()
                .withIdentifier(key)
                .withMetadataFormatPrefix(EML_FORMAT.getMetadataPrefix()));
    assertEquals(key, record.getHeader().getIdentifier());
    assertFalse(
        "An active record is returned and not identified as deleted",
        record.getHeader().isDeleted());

    // deleted the record and make sure it is flagged as 'deleted'
    deleteDataset(org1Installation1Dataset1.getKey());

    record =
        serviceProvider.getRecord(
            GetRecordParameters.request()
                .withIdentifier(key)
                .withMetadataFormatPrefix(EML_FORMAT.getMetadataPrefix()));
    assertEquals(key, record.getHeader().getIdentifier());
    assertTrue(
        "A deleted record is returned and is identified as deleted",
        record.getHeader().isDeleted());

    // restore the dataset
    org1Installation1Dataset1 = getDataset(org1Installation1Dataset1.getKey());
    assertNotNull(org1Installation1Dataset1.getDeleted());

    org1Installation1Dataset1.setDeleted(null);
    updateDataset(org1Installation1Dataset1);
    record =
        serviceProvider.getRecord(
            GetRecordParameters.request()
                .withIdentifier(key)
                .withMetadataFormatPrefix(EML_FORMAT.getMetadataPrefix()));
    assertEquals(key, record.getHeader().getIdentifier());
    assertFalse(
        "A restored record is returned and not identified as deleted",
        record.getHeader().isDeleted());
  }
}
