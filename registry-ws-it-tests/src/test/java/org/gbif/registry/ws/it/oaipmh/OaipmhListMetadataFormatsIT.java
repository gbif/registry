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
import org.gbif.registry.test.TestDataFactory;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Date;
import java.util.List;

import org.dspace.xoai.model.oaipmh.MetadataFormat;
import org.dspace.xoai.serviceprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.serviceprovider.parameters.ListMetadataParameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import com.google.common.collect.Lists;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Test the ListMetadataFormats verb of the OAI-PMH endpoint. */
@SuppressWarnings("unchecked")
public class OaipmhListMetadataFormatsIT extends AbstractOaipmhEndpointIT {

  @Autowired
  public OaipmhListMetadataFormatsIT(
      SimplePrincipalProvider pp,
      Environment environment,
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService,
      DatasetService datasetService,
      TestDataFactory testDataFactory) {
    super(
        pp,
        environment,
        nodeService,
        organizationService,
        installationService,
        datasetService,
        testDataFactory);
  }

  @Test
  public void listMetadataFormats_notFound() {
    assertThrows(
        IdDoesNotExistException.class,
        () ->
            serviceProvider.listMetadataFormats(
                ListMetadataParameters.request().withIdentifier("non-existent-record-identifier")));
  }

  @Test
  public void listMetadataFormats_forARecord() throws Exception {
    Organization org1 = createOrganization(Country.ETHIOPIA);
    Installation org1Installation1 = createInstallation(org1.getKey());
    Dataset org1Installation1Dataset1 =
        createDataset(
            org1.getKey(), org1Installation1.getKey(), DatasetType.OCCURRENCE, new Date());
    assertNotNull(org1Installation1Dataset1.getKey());

    String key = org1Installation1Dataset1.getKey().toString();

    List<MetadataFormat> metadataFormats =
        Lists.newArrayList(
            serviceProvider.listMetadataFormats(
                ListMetadataParameters.request().withIdentifier(key)));

    assertThat(
        "EML and OAIDC formats supported",
        metadataFormats,
        containsInAnyOrder(samePropertyValuesAs(OAIDC_FORMAT), samePropertyValuesAs(EML_FORMAT)));
  }

  @Test
  public void listMetadataFormats_forRepository() throws Exception {

    List<MetadataFormat> metadataFormats =
        Lists.newArrayList(serviceProvider.listMetadataFormats(ListMetadataParameters.request()));

    assertThat(
        "EML and OAIDC formats supported",
        metadataFormats,
        containsInAnyOrder(samePropertyValuesAs(OAIDC_FORMAT), samePropertyValuesAs(EML_FORMAT)));
  }
}
