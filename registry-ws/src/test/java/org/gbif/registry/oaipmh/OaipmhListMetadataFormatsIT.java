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

import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;
import org.dspace.xoai.model.oaipmh.MetadataFormat;
import org.dspace.xoai.serviceprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.serviceprovider.parameters.ListMetadataParameters;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertThat;

/**
 * Test the ListMetadataFormats verb of the OAI-PMH endpoint.
 */
@RunWith(Parameterized.class)
public class OaipmhListMetadataFormatsIT extends AbstractOaipmhEndpointIT {

  private MetadataFormat oaidcFormat = new MetadataFormat()
          .withMetadataPrefix("oai_dc")
          .withMetadataNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/")
          .withSchema("http://www.openarchives.org/OAI/2.0/oai_dc.xsd");

  private MetadataFormat emlFormat = new MetadataFormat()
          .withMetadataPrefix("eml")
          .withMetadataNamespace("eml://ecoinformatics.org/eml-2.1.1")
          .withSchema("http://rs.gbif.org/schema/eml-gbif-profile/1.0.2/eml.xsd");

  public OaipmhListMetadataFormatsIT(NodeService nodeService, OrganizationService organizationService, InstallationService installationService, DatasetService datasetService) {
    super(nodeService, organizationService, installationService, datasetService);
  }

  @Test(expected = IdDoesNotExistException.class)
  public void listMetadataFormats_notFound() throws IdDoesNotExistException {
    serviceProvider.listMetadataFormats(
            ListMetadataParameters.request()
                    .withIdentifier("non-existent-record-identifier")
    );
  }

  @Test
  public void listMetadataFormats_forARecord() throws Exception {
    Organization org1 = createOrganization(Country.ETHIOPIA);
    Installation org1Installation1 = createInstallation(org1.getKey());
    Dataset org1Installation1Dataset1 = createDataset(org1.getKey(), org1Installation1.getKey(), DatasetType.OCCURRENCE, new Date());

    String key = org1Installation1Dataset1.getKey().toString();

    List<MetadataFormat> metadataFormats = Lists.newArrayList(serviceProvider.listMetadataFormats(
            ListMetadataParameters.request()
                    .withIdentifier(key)
    ));

    assertThat("EML and OAIDC formats supported", metadataFormats, Matchers.containsInAnyOrder(oaidcFormat, emlFormat));
  }

  @Test
  public void listMetadataFormats_forRepository() throws Exception {

    List<MetadataFormat> metadataFormats = Lists.newArrayList(serviceProvider.listMetadataFormats(
            ListMetadataParameters.request()
    ));

    assertThat("EML and OAIDC formats supported", metadataFormats, Matchers.containsInAnyOrder(oaidcFormat, emlFormat));
  }
}
