package org.gbif.registry.oaipmh;

import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;

import org.dspace.xoai.model.oaipmh.DeletedRecord;
import org.dspace.xoai.model.oaipmh.Identify;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

/**
 * Test the Identify verb of the OAI-PMH endpoint.
 */
@RunWith(Parameterized.class)
public class OaipmhIdentifyIT extends AbstractOaipmhEndpointIT {

  public OaipmhIdentifyIT(NodeService nodeService, OrganizationService organizationService, InstallationService installationService, DatasetService datasetService) {
    super(nodeService, organizationService, installationService, datasetService);
  }

  @Test
  public void identify() {

    Identify response = serviceProvider.identify();

    assertEquals("GBIF Test Registry", response.getRepositoryName());
    assertEquals(baseUrl, response.getBaseURL());
    assertEquals(DeletedRecord.PERSISTENT, response.getDeletedRecord());
  }
}
