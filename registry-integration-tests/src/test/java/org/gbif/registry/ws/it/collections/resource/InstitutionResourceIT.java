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
import org.gbif.api.model.collections.merge.ConvertToCollectionParams;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.collections.suggestions.ChangeSuggestionService;
import org.gbif.api.model.collections.suggestions.InstitutionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.collections.Discipline;
import org.gbif.api.vocabulary.collections.InstitutionGovernance;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class InstitutionResourceIT
    extends BaseCollectionEntityResourceIT<Institution, InstitutionChangeSuggestion> {

  @MockBean private InstitutionService institutionService;

  @MockBean private InstitutionDuplicatesService institutionDuplicatesService;

  @MockBean private InstitutionMergeService institutionMergeService;

  @MockBean private InstitutionChangeSuggestionService institutionChangeSuggestionService;

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

    InstitutionSearchRequest req = new InstitutionSearchRequest();
    req.setCity("city");
    req.setContact(UUID.randomUUID());
    req.setCountry(Country.DENMARK);
    req.setActive(true);
    req.setInstitutionalGovernance(InstitutionGovernance.ACADEMIC_FEDERAL);
    req.setDisciplines(Arrays.asList(Discipline.AGRICULTURAL, Discipline.OCEAN));

    PagingResponse<Institution> result = getClient().list(req);
    assertEquals(institutions.size(), result.getResults().size());
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

    when(institutionService.listDeleted(any(UUID.class), any(Pageable.class)))
        .thenReturn(
            new PagingResponse<>(
                new PagingRequest(), Long.valueOf(institutions.size()), institutions));

    PagingResponse<Institution> result =
        getClient().listDeleted(UUID.randomUUID(), new PagingRequest());
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

  protected InstitutionClient getClient() {
    return (InstitutionClient) baseClient;
  }

  @Override
  protected CollectionEntityService<Institution> getMockCollectionEntityService() {
    return institutionService;
  }
}
