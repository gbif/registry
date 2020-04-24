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
package org.gbif.registry.ws.it;

import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A test that will populate a sample registry database. This class should be removed when
 * development progresses. This is only used to help those developing the web console.
 */
public class BootstrapIT extends BaseItTest {

  private final TestDataFactory testDataFactory;

  private final NodeService nodeService;

  private final OrganizationService organizationService;

  @Autowired
  public BootstrapIT(
      SimplePrincipalProvider simplePrincipalProvider,
      TestDataFactory testDataFactory,
      NodeService nodeService,
      OrganizationService organizationService) {
    super(simplePrincipalProvider);
    this.testDataFactory = testDataFactory;
    this.nodeService = nodeService;
    this.organizationService = organizationService;
  }

  @Test
  public void run() {
    Node n1 = testDataFactory.newNode();
    n1.setKey(nodeService.create(n1));
    Organization o1 = testDataFactory.newOrganization(n1.getKey());
    organizationService.create(o1);

    Node n2 = testDataFactory.newNode();
    n2.setTitle("The US Node");
    n2.setKey(nodeService.create(n2));
    Organization o2 = testDataFactory.newOrganization(n2.getKey());
    o2.setEndorsementApproved(true);
    organizationService.create(o2);

    String[] tags = {"Abies", "Georeferenced", "Images", "Dubious", "DataPaper"};
    for (String tag : tags) {
      nodeService.addTag(n1.getKey(), tag);
      nodeService.addTag(n2.getKey(), tag);
    }
  }

  @Test
  public void lots() {
    for (int n = 0; n < 100; n++) {
      Node n1 = testDataFactory.newNode();
      n1.setTitle((n + 1) + ": " + n1.getTitle());
      n1.setKey(nodeService.create(n1));
      Organization o1 = testDataFactory.newOrganization(n1.getKey());
      organizationService.create(o1);
      String[] tags = {"Abies", "Georeferenced", "Images", "Dubious", "DataPaper"};
      for (String tag : tags) {
        nodeService.addTag(n1.getKey(), tag);
      }
    }
  }
}
