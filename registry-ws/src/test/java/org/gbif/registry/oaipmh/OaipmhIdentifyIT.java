package org.gbif.registry.oaipmh;

import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;

import org.apache.commons.lang3.StringUtils;
import org.dspace.xoai.model.oaipmh.DeletedRecord;
import org.dspace.xoai.model.oaipmh.Identify;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test the Identify verb of the OAI-PMH endpoint.
 */
@RunWith(Parameterized.class)
public class OaipmhIdentifyIT extends AbstractOaipmhEndpointIT {

  public OaipmhIdentifyIT(NodeService nodeService, OrganizationService organizationService, InstallationService installationService, DatasetService datasetService) {
    super(nodeService, organizationService, installationService, datasetService);
  }

  /**
   * Since the Identity verb response is all set by the OaipmhMockModule (with test parameters)
   * we only test that we actually get a response.
   */
  @Test
  public void identify() {

    Identify response = serviceProvider.identify();

    assertThat("Identity verb send a RepositoryName", response.getRepositoryName(), Matchers.not(Matchers.isEmptyOrNullString()));
    assertThat("Identity verb send a baseUrl", response.getBaseURL(), Matchers.not(Matchers.isEmptyOrNullString()));
    assertEquals(DeletedRecord.PERSISTENT, response.getDeletedRecord());
  }
}
