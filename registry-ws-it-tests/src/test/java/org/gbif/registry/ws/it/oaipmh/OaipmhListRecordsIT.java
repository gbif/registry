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
package org.gbif.registry.ws.it.oaipmh;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.oaipmh.OaipmhSetRepository;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.utils.OaipmhTestConfiguration;
import org.gbif.utils.file.FileUtils;

import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.serviceprovider.parameters.GetRecordParameters;
import org.dspace.xoai.serviceprovider.parameters.ListRecordsParameters;
import org.junit.Test;
import org.springframework.core.env.Environment;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Test the ListRecords verb of the OAI-PMH endpoint. */
public class OaipmhListRecordsIT extends AbstractOaipmhEndpointIT {

  public OaipmhListRecordsIT(
      Environment environment,
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService,
      DatasetService datasetService,
      TestDataFactory testDataFactory) {
    super(
        environment,
        nodeService,
        organizationService,
        installationService,
        datasetService,
        testDataFactory);
  }

  /**
   * Prepare test data
   *
   * @throws Throwable
   */
  public void prepareData() throws Exception {
    Organization org1 = createOrganization(Country.ICELAND);
    Installation org1Installation1 = createInstallation(org1.getKey());
    Dataset org1Installation1Dataset1 =
        createDataset(org1.getKey(), org1Installation1.getKey(), DatasetType.CHECKLIST, new Date());

    Installation org1Installation2 = createInstallation(org1.getKey());
    Dataset org1Installation2Dataset1 =
        createDataset(
            org1.getKey(), org1Installation2.getKey(), DatasetType.OCCURRENCE, new Date());

    Organization org2 = createOrganization(Country.NEW_ZEALAND);
    Installation org2Installation1 = createInstallation(org2.getKey());
    Dataset org2Installation1Dataset1 =
        createDataset(org2.getKey(), org2Installation1.getKey(), DatasetType.CHECKLIST, new Date());
  }

  @Test
  public void testListRecordsBySets() throws Exception {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2015, 9, 16);
    Organization orgIceland = createOrganization(Country.ICELAND);
    Installation orgIcelandInstallation1 = createInstallation(orgIceland.getKey());
    Dataset orgIcelandInstallation1Dataset1 =
        createDataset(
            orgIceland.getKey(),
            orgIcelandInstallation1.getKey(),
            DatasetType.CHECKLIST,
            calendar.getTime());

    Installation orgIcelandInstallation2 = createInstallation(orgIceland.getKey());
    Dataset orgIcelandInstallation2Dataset1 =
        createDataset(
            orgIceland.getKey(),
            orgIcelandInstallation2.getKey(),
            DatasetType.OCCURRENCE,
            new Date());

    Organization org2 = createOrganization(Country.NEW_ZEALAND);
    Installation org2Installation1 = createInstallation(org2.getKey());
    createDataset(org2.getKey(), org2Installation1.getKey(), DatasetType.CHECKLIST, new Date());

    // SetType.COUNTRY
    Iterator<Record> records =
        serviceProvider.listRecords(
            ListRecordsParameters.request()
                .withMetadataPrefix(EML_FORMAT.getMetadataPrefix())
                .withSetSpec(
                    OaipmhSetRepository.SetType.COUNTRY.getSubsetPrefix()
                        + Country.ICELAND.getIso2LetterCode()));
    assertTrue("ListRecords verb with set Country return at least 1 record", records.hasNext());

    // SetType.INSTALLATION
    records =
        serviceProvider.listRecords(
            ListRecordsParameters.request()
                .withMetadataPrefix(EML_FORMAT.getMetadataPrefix())
                .withSetSpec(
                    OaipmhSetRepository.SetType.INSTALLATION.getSubsetPrefix()
                        + orgIcelandInstallation1.getKey().toString()));
    assertTrue(
        "ListRecords verb with set Installation return at least 1 record", records.hasNext());
    assertEquals(
        orgIcelandInstallation1Dataset1.getKey().toString(),
        records.next().getHeader().getIdentifier());

    // SetType.DATASET_TYPE
    records =
        serviceProvider.listRecords(
            ListRecordsParameters.request()
                .withMetadataPrefix(EML_FORMAT.getMetadataPrefix())
                .withSetSpec(
                    OaipmhSetRepository.SetType.DATASET_TYPE.getSubsetPrefix()
                        + DatasetType.OCCURRENCE.toString()));
    assertTrue("ListRecords verb with set DatasetType return at least 1 record", records.hasNext());
    assertEquals(
        orgIcelandInstallation2Dataset1.getKey().toString(),
        records.next().getHeader().getIdentifier());

    // Non-existing Set
    records =
        serviceProvider.listRecords(
            ListRecordsParameters.request()
                .withMetadataPrefix(EML_FORMAT.getMetadataPrefix())
                .withSetSpec("non-existing-set"));
    assertFalse(
        "ListRecords verb with non-existing set should return no record", records.hasNext());
  }

  /**
   * Test that ListRecords verb return all records when the number of records is higher than
   * 'MaxListRecords'. When the number of records is higher than 'MaxListRecords', at least 2
   * requests will be sent and a 'resumptionToken' will be used.
   *
   * @throws Exception
   */
  @Test
  public void testListRecordsPaging() throws Exception {
    int numberOfDataset = 3;
    Organization orgIceland = createOrganization(Country.ICELAND);
    Installation orgIcelandInstallation1 = createInstallation(orgIceland.getKey());
    createDataset(
        orgIceland.getKey(), orgIcelandInstallation1.getKey(), DatasetType.CHECKLIST, new Date());

    Installation orgIcelandInstallation2 = createInstallation(orgIceland.getKey());
    createDataset(
        orgIceland.getKey(), orgIcelandInstallation2.getKey(), DatasetType.OCCURRENCE, new Date());

    Organization org2 = createOrganization(Country.NEW_ZEALAND);
    Installation org2Installation1 = createInstallation(org2.getKey());
    createDataset(org2.getKey(), org2Installation1.getKey(), DatasetType.CHECKLIST, new Date());

    // ensure the test will run under the expected configuration
    assertTrue(
        "'MaxListRecords' should be set to a value less than " + numberOfDataset,
        numberOfDataset > OaipmhTestConfiguration.MAX_LIST_RECORDS);

    Iterator<Record> records =
        serviceProvider.listRecords(
            ListRecordsParameters.request().withMetadataPrefix(EML_FORMAT.getMetadataPrefix()));
    List<Record> recordList = Lists.newArrayList(records);
    assertEquals(
        "ListRecords verb return all records when the number of records is higher than 'MaxListRecords'",
        numberOfDataset,
        recordList.size());
  }

  @Test
  public void getListRecordsWithAugmentedMetadata() throws Exception {
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
            new URI(baseUrl + "?verb=ListRecords&metadataPrefix=" + EML_FORMAT.getMetadataPrefix())
                .toURL(),
            "UTF-8");

    // ensure we get the augmented metadata data
    assertTrue(
        "ListRecords verb returns augmented metadata",
        result.contains(
            "<citation identifier=\"doi:tims-ident.2136.ex43.33.d\">title 1</citation>"));
  }
}
