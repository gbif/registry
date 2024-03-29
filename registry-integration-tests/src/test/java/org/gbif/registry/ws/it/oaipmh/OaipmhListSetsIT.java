/*
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

import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.oaipmh.OaipmhSetRepository.SetType;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.xoai.model.oaipmh.Set;
import org.dspace.xoai.serviceprovider.exceptions.NoSetHierarchyException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import com.google.common.collect.Lists;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test the ListSets verb of the OAI-PMH endpoint. */
public class OaipmhListSetsIT extends AbstractOaipmhEndpointIT {

  // OaipmhSetRepository.SetType represents the root of the set hierarchy to the respective node,
  // DatasetType.values() represents the static subsets of OaipmhSetRepository.SetType.DATASET_TYPE
  private static final int NUMBER_OF_STATIC_SETS =
      SetType.values().length + DatasetType.values().length;

  @Autowired
  public OaipmhListSetsIT(
      SimplePrincipalProvider principalProvider,
      Environment environment,
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService,
      DatasetService datasetService,
      TestDataFactory testDataFactory,
      EsManageServer esServer) {
    super(
        principalProvider,
        environment,
        nodeService,
        organizationService,
        installationService,
        datasetService,
        testDataFactory,
        esServer);
  }

  @Test
  public void testSetsWithEmptyDatabase() throws NoSetHierarchyException {
    Iterator<Set> sets = serviceProvider.listSets();
    List<Set> setsList = Lists.newArrayList(sets);

    assertEquals(
        NUMBER_OF_STATIC_SETS,
        setsList.size(),
        "ListSets verb returns only static Sets when database is empty");
  }

  @Test
  public void testSetsWithDatabaseContent() throws Exception {
    Organization orgIceland = createOrganization(Country.ICELAND);
    Installation orgIcelandInstallation1 = createInstallation(orgIceland.getKey());
    assertNotNull(orgIcelandInstallation1.getKey());

    Iterator<Set> sets = serviceProvider.listSets();
    List<Set> setsList = Lists.newArrayList(sets);
    assertEquals(
        NUMBER_OF_STATIC_SETS,
        setsList.size(),
        "ListSets verb returns only static Sets when there is not datasets");

    createDataset(
        orgIceland.getKey(), orgIcelandInstallation1.getKey(), DatasetType.OCCURRENCE, new Date());

    // refresh data
    sets = serviceProvider.listSets();
    setsList = Lists.newArrayList(sets);

    List<String> setsSpecList = setsList.stream().map(Set::getSpec).collect(Collectors.toList());

    assertTrue(
        setsSpecList.contains(
            SetType.COUNTRY.getSubsetPrefix() + Country.ICELAND.getIso2LetterCode()),
        "ListSets verb returns a Set corresponding to the country of the datatset publishing Organization");

    assertTrue(
        setsSpecList.contains(
            SetType.INSTALLATION.getSubsetPrefix() + orgIcelandInstallation1.getKey().toString()),
        "ListSets verb returns a Set corresponding to the Installation from which the datatset was published");
  }
}
