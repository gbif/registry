package org.gbif.registry.oaipmh;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.ws.resources.OccurrenceDownloadResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Date;

import javax.xml.transform.TransformerConfigurationException;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.serviceprovider.ServiceProvider;
import org.dspace.xoai.serviceprovider.client.HttpOAIClient;
import org.dspace.xoai.serviceprovider.client.OAIClient;
import org.dspace.xoai.serviceprovider.exceptions.BadArgumentException;
import org.dspace.xoai.serviceprovider.exceptions.CannotDisseminateFormatException;
import org.dspace.xoai.serviceprovider.exceptions.HarvestException;
import org.dspace.xoai.serviceprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.serviceprovider.model.Context;
import org.dspace.xoai.serviceprovider.parameters.GetRecordParameters;
import org.dspace.xoai.xml.XSLPipeline;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.gbif.registry.guice.RegistryTestModules.webservice;
import static org.gbif.registry.guice.RegistryTestModules.webserviceClient;

import static org.junit.Assert.assertEquals;

/**
 * Test the OAI-PMH endpoint using the XOAI OAI-PMH client library.
 * Test the GetRecord verb of the OAI-PMH endpoint.
 */
@RunWith(Parameterized.class)
public class OaipmhGetRecordIT extends AbstractOaipmhEndpointIT {

  public OaipmhGetRecordIT(NodeService nodeService, OrganizationService organizationService, InstallationService installationService, DatasetService datasetService) {
    super(nodeService, organizationService, installationService, datasetService);
  }

  @Test(expected = IdDoesNotExistException.class)
  public void getRecord_notFound() throws Throwable {

    serviceProvider.getRecord(
            GetRecordParameters.request()
                    .withIdentifier("non-existent-record-identifier")
                    .withMetadataFormatPrefix(EML_FORMAT.getMetadataPrefix())
    );
  }

  @Test(expected = CannotDisseminateFormatException.class)
  public void getRecord_unsuppertedMetadataFormat () throws Exception {
    Organization org1 = createOrganization(Country.UNITED_KINGDOM);
    Installation org1Installation1 = createInstallation(org1.getKey());
    Dataset org1Installation1Dataset1 = createDataset(org1.getKey(), org1Installation1.getKey(), DatasetType.CHECKLIST, new Date());

    String key = org1Installation1Dataset1.getKey().toString();

    serviceProvider.getRecord(GetRecordParameters.request().withIdentifier(key).withMetadataFormatPrefix("made-up-metadata-format"));
  }

  @Test
  public void getRecord_found() throws Exception {
    Organization org1 = createOrganization(Country.ZAMBIA);
    Installation org1Installation1 = createInstallation(org1.getKey());
    Dataset org1Installation1Dataset1 = createDataset(org1.getKey(), org1Installation1.getKey(), DatasetType.CHECKLIST, new Date());

    String key = org1Installation1Dataset1.getKey().toString();

    Record record = serviceProvider.getRecord(
            GetRecordParameters.request()
                    .withIdentifier(key)
                    .withMetadataFormatPrefix(EML_FORMAT.getMetadataPrefix())
    );

    assertEquals(key, record.getHeader().getIdentifier());
  }
}
