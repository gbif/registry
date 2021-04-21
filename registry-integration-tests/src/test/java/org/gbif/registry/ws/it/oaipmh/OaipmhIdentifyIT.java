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

import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import javax.sql.DataSource;

import org.dspace.xoai.model.oaipmh.DeletedRecord;
import org.dspace.xoai.model.oaipmh.Identify;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;

/** Test the Identify verb of the OAI-PMH endpoint. */
public class OaipmhIdentifyIT extends AbstractOaipmhEndpointIT {

  @Autowired
  public OaipmhIdentifyIT(
    SimplePrincipalProvider principalProvider,
    Environment environment,
    NodeService nodeService,
    OrganizationService organizationService,
    InstallationService installationService,
    DatasetService datasetService,
    TestDataFactory testDataFactory,
    EsManageServer esServer,
    DataSource dataSource
    ) {
    super(
        principalProvider,
        environment,
        nodeService,
        organizationService,
        installationService,
        datasetService,
        testDataFactory,
        esServer,
        dataSource);
  }

  /**
   * Since the Identity verb response is all set by the OaipmhTestConfiguration (with test
   * parameters) we only test that we actually get a response.
   */
  @Test
  public void identify() {
    Identify response = serviceProvider.identify();
    assertThat(
        "Identity verb send a RepositoryName",
        response.getRepositoryName(),
        Matchers.not(is(emptyOrNullString())));
    assertThat(
        "Identity verb send a baseUrl",
        response.getBaseURL(),
        Matchers.not(is(emptyOrNullString())));
    assertEquals(DeletedRecord.PERSISTENT, response.getDeletedRecord());
  }
}
