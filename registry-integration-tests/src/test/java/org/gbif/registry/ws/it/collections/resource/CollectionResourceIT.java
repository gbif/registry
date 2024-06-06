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

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionImportParams;
import org.gbif.api.model.collections.latimercore.ObjectGroup;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.BatchService;
import org.gbif.api.service.collections.ChangeSuggestionService;
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.GbifRegion;
import org.gbif.registry.service.collections.batch.CollectionBatchService;
import org.gbif.registry.service.collections.duplicates.CollectionDuplicatesService;
import org.gbif.registry.service.collections.duplicates.DuplicatesService;
import org.gbif.registry.service.collections.merge.CollectionMergeService;
import org.gbif.registry.service.collections.merge.MergeService;
import org.gbif.registry.service.collections.suggestions.CollectionChangeSuggestionService;
import org.gbif.registry.ws.client.collections.CollectionClient;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

public class CollectionResourceIT
    extends BaseCollectionEntityResourceIT<Collection, CollectionChangeSuggestion> {

  @MockBean private CollectionService collectionService;

  @MockBean private CollectionDuplicatesService collectionDuplicatesService;

  @MockBean private CollectionMergeService collectionMergeService;

  @MockBean private CollectionChangeSuggestionService collectionChangeSuggestionService;

  @MockBean private CollectionBatchService collectionBatchService;

  @Autowired
  public CollectionResourceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      RequestTestFixture requestTestFixture,
      @LocalServerPort int localServerPort) {
    super(
        CollectionClient.class,
        simplePrincipalProvider,
        requestTestFixture,
        Collection.class,
        localServerPort);
  }

  @Test
  public void listTest() {
    Collection c1 = testData.newEntity();
    Collection c2 = testData.newEntity();
    List<CollectionView> views =
        Arrays.asList(c1, c2).stream().map(CollectionView::new).collect(Collectors.toList());

    when(collectionService.list(any(CollectionSearchRequest.class)))
        .thenReturn(new PagingResponse<>(new PagingRequest(), Long.valueOf(views.size()), views));

    CollectionSearchRequest req = new CollectionSearchRequest();
    req.setCity("city");
    req.setInstitution(UUID.randomUUID());
    req.setCountry(Collections.singletonList(Country.DENMARK));
    req.setGbifRegion(Collections.singletonList(GbifRegion.EUROPE));
    req.setPersonalCollection(true);
    req.setAccessionStatus("Institutional");
    req.setPreservationTypes(Arrays.asList("SampleCryopreserved", "SampleDried"));
    PagingResponse<CollectionView> result = getClient().list(req);
    assertEquals(views.size(), result.getResults().size());
  }

  @Test
  public void listAndGetAsLatimerCoreTest() {
    ObjectGroup o1 = new ObjectGroup();
    o1.setDescription("des");
    o1.setCollectionName("name");
    ObjectGroup o2 = new ObjectGroup();
    o2.setDescription("des2");
    o2.setCollectionName("name2");
    List<ObjectGroup> orgs = Arrays.asList(o1, o2);

    when(collectionService.listAsLatimerCore(any(CollectionSearchRequest.class)))
        .thenReturn(new PagingResponse<>(new PagingRequest(), Long.valueOf(orgs.size()), orgs));

    PagingResponse<ObjectGroup> result =
        getClient().listAsLatimerCore(new CollectionSearchRequest());
    assertEquals(orgs.size(), result.getResults().size());

    when(collectionService.getAsLatimerCore(any(UUID.class))).thenReturn(o1);
    ObjectGroup objectGroupReturned = getClient().getAsLatimerCore(UUID.randomUUID());
    assertEquals(o1, objectGroupReturned);
  }

  @Test
  public void createAndUpdateLatimerCoreTest() {
    ObjectGroup o1 = new ObjectGroup();
    o1.setDescription("des");
    o1.setCollectionName("name");
    UUID key = UUID.randomUUID();

    when(collectionService.createFromLatimerCore(o1)).thenReturn(key);

    assertEquals(key, getClient().createFromLatimerCore(o1));

    doNothing().when(collectionService).updateFromLatimerCore(o1);
    assertDoesNotThrow(() -> getClient().updateFromLatimerCore(key, o1));
  }

  @Test
  public void testSuggest() {
    KeyCodeNameResult r1 = new KeyCodeNameResult(UUID.randomUUID(), "c1", "n1");
    KeyCodeNameResult r2 = new KeyCodeNameResult(UUID.randomUUID(), "c2", "n2");
    List<KeyCodeNameResult> results = Arrays.asList(r1, r2);

    when(collectionService.suggest(anyString())).thenReturn(results);
    assertEquals(2, getClient().suggest("foo").size());
  }

  @Test
  public void listDeletedTest() {
    Collection c1 = testData.newEntity();
    c1.setKey(UUID.randomUUID());
    c1.setCode("code1");
    c1.setName("Collection name");

    Collection c2 = testData.newEntity();
    c2.setKey(UUID.randomUUID());
    c2.setCode("code2");
    c2.setName("Collection name2");

    List<CollectionView> views =
        Arrays.asList(c1, c2).stream().map(CollectionView::new).collect(Collectors.toList());

    when(collectionService.listDeleted(any(CollectionSearchRequest.class)))
        .thenReturn(new PagingResponse<>(new PagingRequest(), Long.valueOf(views.size()), views));

    CollectionSearchRequest request = new CollectionSearchRequest();
    request.setReplacedBy(UUID.randomUUID());
    PagingResponse<CollectionView> result = getClient().listDeleted(request);
    assertEquals(views.size(), result.getResults().size());
  }

  @Override
  protected DuplicatesService getMockDuplicatesService() {
    return collectionDuplicatesService;
  }

  @Override
  protected MergeService<Collection> getMockMergeService() {
    return collectionMergeService;
  }

  @Override
  protected ChangeSuggestionService<Collection, CollectionChangeSuggestion>
      getMockChangeSuggestionService() {
    return collectionChangeSuggestionService;
  }

  @Override
  protected CollectionChangeSuggestion newChangeSuggestion() {
    CollectionChangeSuggestion changeSuggestion = new CollectionChangeSuggestion();
    changeSuggestion.setType(Type.CREATE);
    changeSuggestion.setComments(Collections.singletonList("comment"));
    changeSuggestion.setProposedBy("aaa@aa.com");

    Collection c1 = new Collection();
    c1.setCode("c1");
    c1.setName("name1");
    c1.setActive(true);
    changeSuggestion.setSuggestedEntity(c1);

    return changeSuggestion;
  }

  @Override
  protected BatchService getBatchService() {
    return collectionBatchService;
  }

  @Test
  public void createFromDatasetTest() {
    UUID institutionKey = UUID.randomUUID();
    when(collectionService.createFromDataset(any(), any())).thenReturn(institutionKey);

    CollectionImportParams params = new CollectionImportParams();
    params.setDatasetKey(UUID.randomUUID());
    params.setCollectionCode("code");
    assertEquals(institutionKey, getClient().createFromDataset(params));
  }

  protected CollectionClient getClient() {
    return (CollectionClient) baseClient;
  }

  @Override
  void mockGetEntity(UUID key, Collection entityToReturn) {
    when(collectionService.getCollectionView(key)).thenReturn(new CollectionView(entityToReturn));
  }

  @Override
  protected CollectionEntityService<Collection> getMockCollectionEntityService() {
    return collectionService;
  }
}
