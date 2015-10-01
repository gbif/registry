package org.gbif.registry.oaipmh;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.guice.OaipmhMockModule;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import com.google.inject.matcher.Matcher;
import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.serviceprovider.parameters.ListRecordsParameters;
import org.hamcrest.Factory;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test the ListRecords verb of the OAI-PMH endpoint.
 *
 */
@RunWith(Parameterized.class)
public class OaipmhListRecordsIT extends AbstractOaipmhEndpointIT {

  public OaipmhListRecordsIT(NodeService nodeService, OrganizationService organizationService, InstallationService installationService, DatasetService datasetService) {
    super(nodeService, organizationService, installationService, datasetService);
  }

  /**
   * Prepare test data
   * @throws Throwable
   */
  public void prepareData() throws Exception {
    Organization org1 = createOrganization(Country.ICELAND);
    Installation org1Installation1 = createInstallation(org1.getKey());
    Dataset org1Installation1Dataset1 = createDataset(org1.getKey(), org1Installation1.getKey(), DatasetType.CHECKLIST, new Date());

    Installation org1Installation2 = createInstallation(org1.getKey());
    Dataset org1Installation2Dataset1 = createDataset(org1.getKey(), org1Installation2.getKey(), DatasetType.OCCURRENCE, new Date());

    Organization org2 = createOrganization(Country.NEW_ZEALAND);
    Installation org2Installation1 = createInstallation(org2.getKey());
    Dataset org2Installation1Dataset1 = createDataset(org2.getKey(), org2Installation1.getKey(), DatasetType.CHECKLIST, new Date());
  }

  @Test
  public void testListRecordsBySets() throws Exception {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2015,9,16);
    Organization orgIceland = createOrganization(Country.ICELAND);
    Installation orgIcelandInstallation1 = createInstallation(orgIceland.getKey());
    Dataset orgIcelandInstallation1Dataset1 = createDataset(orgIceland.getKey(), orgIcelandInstallation1.getKey(), DatasetType.CHECKLIST, calendar.getTime());

    Installation orgIcelandInstallation2 = createInstallation(orgIceland.getKey());
    Dataset orgIcelandInstallation2Dataset1 = createDataset(orgIceland.getKey(), orgIcelandInstallation2.getKey(), DatasetType.OCCURRENCE, new Date());

    Organization org2 = createOrganization(Country.NEW_ZEALAND);
    Installation org2Installation1 = createInstallation(org2.getKey());
    createDataset(org2.getKey(), org2Installation1.getKey(), DatasetType.CHECKLIST, new Date());

    // SetType.COUNTRY
    Iterator<Record> records = serviceProvider.listRecords(
            ListRecordsParameters.request()
                    .withMetadataPrefix(EML_FORMAT.getMetadataPrefix())
                    .withSetSpec(OaipmhSetRepository.SetType.COUNTRY.getSubsetPrefix() + Country.ICELAND.getIso2LetterCode()));
    assertTrue("ListRecords verb with set Country return at least 1 record", records.hasNext());

    // SetType.INSTALLATION
    records = serviceProvider.listRecords(
            ListRecordsParameters.request()
                    .withMetadataPrefix(EML_FORMAT.getMetadataPrefix())
                    .withSetSpec(OaipmhSetRepository.SetType.INSTALLATION.getSubsetPrefix() + orgIcelandInstallation1.getKey().toString()));
    assertTrue("ListRecords verb with set Installation return at least 1 record", records.hasNext());
    assertEquals(orgIcelandInstallation1Dataset1.getKey().toString(), records.next().getHeader().getIdentifier());

    // SetType.DATASET_TYPE
    records = serviceProvider.listRecords(
            ListRecordsParameters.request()
                    .withMetadataPrefix(EML_FORMAT.getMetadataPrefix())
                    .withSetSpec(OaipmhSetRepository.SetType.DATASET_TYPE.getSubsetPrefix() + DatasetType.OCCURRENCE.toString()));
    assertTrue("ListRecords verb with set DatasetType return at least 1 record", records.hasNext());
    assertEquals(orgIcelandInstallation2Dataset1.getKey().toString(), records.next().getHeader().getIdentifier());

    // Non-existing Set
    records = serviceProvider.listRecords(
            ListRecordsParameters.request()
                    .withMetadataPrefix(EML_FORMAT.getMetadataPrefix())
                    .withSetSpec("non-existing-set"));
    assertFalse("ListRecords verb with non-existing set should return no record", records.hasNext());
  }

  /**
   *
   * Test that ListRecords verb return all records when the number of records is higher than 'MaxListRecords'.
   * When the number of records is higher than 'MaxListRecords', at least 2 requests will be sent and a 'resumptionToken'
   * will be used.
   *
   * @throws Exception
   */
  @Test
  public void testListRecordsPaging() throws Exception {
    int numberOfDataset = 3;
    Organization orgIceland = createOrganization(Country.ICELAND);
    Installation orgIcelandInstallation1 = createInstallation(orgIceland.getKey());
    createDataset(orgIceland.getKey(), orgIcelandInstallation1.getKey(), DatasetType.CHECKLIST, new Date());

    Installation orgIcelandInstallation2 = createInstallation(orgIceland.getKey());
    createDataset(orgIceland.getKey(), orgIcelandInstallation2.getKey(), DatasetType.OCCURRENCE, new Date());

    Organization org2 = createOrganization(Country.NEW_ZEALAND);
    Installation org2Installation1 = createInstallation(org2.getKey());
    createDataset(org2.getKey(), org2Installation1.getKey(), DatasetType.CHECKLIST, new Date());

    // ensure the test will run under the expected configuration
    assertTrue("OaipmhMockModule 'MaxListRecords' should be set to a value less than " + numberOfDataset, numberOfDataset > OaipmhMockModule.MAX_LIST_RECORDS);

    Iterator<Record> records = serviceProvider.listRecords(
            ListRecordsParameters.request()
                    .withMetadataPrefix(EML_FORMAT.getMetadataPrefix()));

    assertThat("ListRecords verb return all records when the number of records is higher than 'MaxListRecords'", records, IsIterorWithSize.<Record>iteratorWithSize(numberOfDataset));
  }

}
