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
package org.gbif.registry.ws.it.collections.resource;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.InstitutionImportParams;
import org.gbif.api.model.collections.latimercore.OrganisationalUnit;
import org.gbif.api.model.collections.merge.ConvertToCollectionParams;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.collections.suggestions.InstitutionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.BatchService;
import org.gbif.api.service.collections.ChangeSuggestionService;
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.GbifRegion;
import org.gbif.registry.service.collections.batch.InstitutionBatchService;
import org.gbif.registry.service.collections.duplicates.DuplicatesService;
import org.gbif.registry.service.collections.duplicates.InstitutionDuplicatesService;
import org.gbif.registry.service.collections.merge.InstitutionMergeService;
import org.gbif.registry.service.collections.merge.MergeService;
import org.gbif.registry.service.collections.suggestions.InstitutionChangeSuggestionService;
import org.gbif.registry.ws.client.collections.InstitutionClient;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.Point;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class InstitutionResourceIT
    extends BaseCollectionEntityResourceIT<Institution, InstitutionChangeSuggestion> {

  @MockBean private InstitutionService institutionService;

  @MockBean private InstitutionDuplicatesService institutionDuplicatesService;

  @MockBean private InstitutionMergeService institutionMergeService;

  @MockBean private InstitutionChangeSuggestionService institutionChangeSuggestionService;

  @MockBean private InstitutionBatchService institutionBatchService;

  @Autowired
  public InstitutionResourceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      RequestTestFixture requestTestFixture,
      @LocalServerPort int localServerPort) {
    super(
        InstitutionClient.class,
        simplePrincipalProvider,
        requestTestFixture,
        Institution.class,
        localServerPort);
  }

  @Test
  public void listTest() {
    Institution i1 = testData.newEntity();
    Institution i2 = testData.newEntity();
    List<Institution> institutions = Arrays.asList(i1, i2);

    when(institutionService.list(any(InstitutionSearchRequest.class)))
        .thenReturn(
            new PagingResponse<>(
                new PagingRequest(), Long.valueOf(institutions.size()), institutions));

    InstitutionSearchRequest req = InstitutionSearchRequest.builder().build();
    req.setCity(Collections.singletonList("city"));
    req.setContact(Collections.singletonList(UUID.randomUUID()));
    req.setCountry(Collections.singletonList(Country.DENMARK));
    req.setGbifRegion(Collections.singletonList(GbifRegion.EUROPE));
    req.setActive(Collections.singletonList(true));
    req.setInstitutionalGovernance(Collections.singletonList("Academic"));
    req.setDisciplines(Arrays.asList("Archaeology", "Anthropology"));

    PagingResponse<Institution> result = getClient().list(req);
    assertEquals(institutions.size(), result.getResults().size());
  }

  @Test
  public void listAndGetAsLatimerCoreTest() {
    OrganisationalUnit o1 = new OrganisationalUnit();
    o1.setOrganisationalUnitName("Test");
    o1.setOrganisationalUnitType("Institution");
    OrganisationalUnit o2 = new OrganisationalUnit();
    o2.setOrganisationalUnitName("Test2");
    o2.setOrganisationalUnitType("Institution");
    List<OrganisationalUnit> orgs = Arrays.asList(o1, o2);

    when(institutionService.listAsLatimerCore(any(InstitutionSearchRequest.class)))
        .thenReturn(new PagingResponse<>(new PagingRequest(), Long.valueOf(orgs.size()), orgs));

    PagingResponse<OrganisationalUnit> result =
        getClient().listAsLatimerCore(InstitutionSearchRequest.builder().build());
    assertEquals(orgs.size(), result.getResults().size());

    when(institutionService.getAsLatimerCore(any(UUID.class))).thenReturn(o1);
    OrganisationalUnit organisationalUnitReturned = getClient().getAsLatimerCore(UUID.randomUUID());
    assertEquals(o1, organisationalUnitReturned);
  }

  @Test
  public void createAndUpdateLatimerCoreTest() {
    OrganisationalUnit o1 = new OrganisationalUnit();
    o1.setOrganisationalUnitName("Test");
    o1.setOrganisationalUnitType("Institution");
    UUID key = UUID.randomUUID();

    when(institutionService.createFromLatimerCore(o1)).thenReturn(key);

    assertEquals(key, getClient().createFromLatimerCore(o1));

    doNothing().when(institutionService).updateFromLatimerCore(o1);
    assertDoesNotThrow(() -> getClient().updateFromLatimerCore(key, o1));
  }

  @Test
  public void listAsGeoJsonTest() {
    FeatureCollection featureCollection = new FeatureCollection();
    Feature f1 = new Feature();
    f1.setGeometry(new Point(12d, 50d));
    f1.setProperty("name", "n1");
    f1.setProperty("key", UUID.randomUUID().toString());
    featureCollection.add(f1);
    Feature f2 = new Feature();
    f2.setGeometry(new Point(22d, 51d));
    f2.setProperty("name", "n2");
    f2.setProperty("key", UUID.randomUUID().toString());
    featureCollection.add(f2);

    when(institutionService.listGeojson(any(InstitutionSearchRequest.class)))
        .thenReturn(featureCollection);

    FeatureCollection result =
        getClient().listAsGeoJson(InstitutionSearchRequest.builder().build());
    assertEquals(featureCollection.getFeatures().size(), result.getFeatures().size());
  }

  @Test
  public void testSuggest() {
    KeyCodeNameResult r1 = new KeyCodeNameResult(UUID.randomUUID(), "c1", "n1");
    KeyCodeNameResult r2 = new KeyCodeNameResult(UUID.randomUUID(), "c2", "n2");
    List<KeyCodeNameResult> results = Arrays.asList(r1, r2);

    when(institutionService.suggest(anyString())).thenReturn(results);
    assertEquals(2, getClient().suggest("foo").size());
  }

  @Test
  public void listDeletedTest() {
    Institution i1 = testData.newEntity();
    i1.setKey(UUID.randomUUID());
    i1.setCode("code1");
    i1.setName("Institution name");

    Institution i2 = testData.newEntity();
    i2.setKey(UUID.randomUUID());
    i2.setCode("code2");
    i2.setName("Institution name2");

    List<Institution> institutions = Arrays.asList(i1, i2);

    when(institutionService.listDeleted(any(InstitutionSearchRequest.class)))
        .thenReturn(
            new PagingResponse<>(
                new PagingRequest(), Long.valueOf(institutions.size()), institutions));

    InstitutionSearchRequest request = InstitutionSearchRequest.builder().build();
    request.setReplacedBy(Collections.singletonList(UUID.randomUUID()));
    PagingResponse<Institution> result = getClient().listDeleted(request);
    assertEquals(institutions.size(), result.getResults().size());
  }

  @Test
  public void convertToCollectionTest() {
    UUID convertedCollectionKey = UUID.randomUUID();
    when(institutionMergeService.convertToCollection(any(UUID.class), any(UUID.class), anyString()))
        .thenReturn(convertedCollectionKey);

    ConvertToCollectionParams params = new ConvertToCollectionParams();
    params.setInstitutionForNewCollectionKey(UUID.randomUUID());
    params.setNameForNewInstitution("name");

    assertEquals(
        convertedCollectionKey, getClient().convertToCollection(UUID.randomUUID(), params));
  }

  @Test
  public void createFromOrganizationTest() {
    UUID collectionKey = UUID.randomUUID();
    when(institutionService.createFromOrganization(any(), any())).thenReturn(collectionKey);

    InstitutionImportParams params = new InstitutionImportParams();
    params.setOrganizationKey(UUID.randomUUID());
    params.setInstitutionCode("code");
    assertEquals(collectionKey, getClient().createFromOrganization(params));
  }

  @Override
  protected DuplicatesService getMockDuplicatesService() {
    return institutionDuplicatesService;
  }

  @Override
  protected MergeService<Institution> getMockMergeService() {
    return institutionMergeService;
  }

  @Override
  protected ChangeSuggestionService<Institution, InstitutionChangeSuggestion>
      getMockChangeSuggestionService() {
    return institutionChangeSuggestionService;
  }

  @Override
  protected InstitutionChangeSuggestion newChangeSuggestion() {
    InstitutionChangeSuggestion changeSuggestion = new InstitutionChangeSuggestion();
    changeSuggestion.setType(Type.CREATE);
    changeSuggestion.setComments(Collections.singletonList("comment"));
    changeSuggestion.setProposedBy("aaa@aa.com");

    Institution i1 = new Institution();
    i1.setCode("i1");
    i1.setName("name1");
    i1.setActive(true);
    changeSuggestion.setSuggestedEntity(i1);

    return changeSuggestion;
  }

  @Override
  protected BatchService getBatchService() {
    return institutionBatchService;
  }

  protected InstitutionClient getClient() {
    return (InstitutionClient) baseClient;
  }

  @Override
  protected CollectionEntityService<Institution> getMockCollectionEntityService() {
    return institutionService;
  }
}
