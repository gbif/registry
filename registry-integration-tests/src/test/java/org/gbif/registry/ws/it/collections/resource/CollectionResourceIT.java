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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionImportParams;
import org.gbif.api.model.collections.descriptors.Descriptor;
import org.gbif.api.model.collections.descriptors.DescriptorChangeSuggestion;
import org.gbif.api.model.collections.descriptors.DescriptorChangeSuggestionRequest;
import org.gbif.api.model.collections.descriptors.DescriptorGroup;
import org.gbif.api.model.collections.latimercore.ObjectGroup;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.request.DescriptorGroupSearchRequest;
import org.gbif.api.model.collections.request.DescriptorSearchRequest;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.BatchService;
import org.gbif.api.service.collections.ChangeSuggestionService;
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.DescriptorChangeSuggestionService;
import org.gbif.api.service.collections.DescriptorsService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.GbifRegion;
import org.gbif.api.vocabulary.Rank;
import org.gbif.registry.service.collections.batch.CollectionBatchService;
import org.gbif.registry.service.collections.duplicates.CollectionDuplicatesService;
import org.gbif.registry.service.collections.duplicates.DuplicatesService;
import org.gbif.registry.service.collections.merge.CollectionMergeService;
import org.gbif.registry.service.collections.merge.MergeService;
import org.gbif.registry.service.collections.suggestions.CollectionChangeSuggestionService;
import org.gbif.registry.ws.client.collections.CollectionClient;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

public class CollectionResourceIT
    extends BaseCollectionEntityResourceIT<Collection, CollectionChangeSuggestion> {

  @MockBean private CollectionService collectionService;

  @MockBean private CollectionDuplicatesService collectionDuplicatesService;

  @MockBean private CollectionMergeService collectionMergeService;

  @MockBean private CollectionChangeSuggestionService collectionChangeSuggestionService;

  @MockBean private CollectionBatchService collectionBatchService;

  @MockBean private DescriptorsService descriptorsService;

  @MockBean private DescriptorChangeSuggestionService descriptorChangeSuggestionService;

  @MockBean private InstitutionService institutionService;

  @Captor
  private ArgumentCaptor<InstitutionSearchRequest> searchRequestCaptor;

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

    CollectionSearchRequest req = CollectionSearchRequest.builder().build();
    req.setCity(Collections.singletonList("city"));
    req.setInstitution(Collections.singletonList(UUID.randomUUID()));
    req.setCountry(Collections.singletonList(Country.DENMARK));
    req.setGbifRegion(Collections.singletonList(GbifRegion.EUROPE));
    req.setPersonalCollection(Collections.singletonList(true));
    req.setAccessionStatus(Collections.singletonList("Institutional"));
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
        getClient().listAsLatimerCore(CollectionSearchRequest.builder().build());
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

    CollectionSearchRequest request = CollectionSearchRequest.builder().build();
    request.setReplacedBy(Collections.singletonList(UUID.randomUUID()));
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

  @SneakyThrows
  @Test
  public void createDescriptorGroupTest() {
    when(descriptorsService.createDescriptorGroup(any(), any(), any(), any(), any(), any()))
        .thenReturn(1L);

    Resource descriptorsResource = new ClassPathResource("collections/descriptors.csv");
    MultipartFile descriptorsFile =
        new MockMultipartFile("descriptorsFile", descriptorsResource.getInputStream());

    assertEquals(
        1L,
        getClient()
            .createDescriptorGroup(
                UUID.randomUUID(), ExportFormat.CSV, descriptorsFile, "title",  "desc", Set.of("test-tag")));
  }

  @SneakyThrows
  @Test
  public void updateDescriptorGroupTest() {
    UUID collectionKey = UUID.randomUUID();
    DescriptorGroup descriptorGroup = new DescriptorGroup();
    descriptorGroup.setCollectionKey(collectionKey);
    descriptorGroup.setTitle("title");

    when(descriptorsService.getDescriptorGroup(anyLong())).thenReturn(descriptorGroup);
    doNothing()
        .when(descriptorsService)
        .updateDescriptorGroup(anyLong(), any(), any(), anyString(), any(Set.class), anyString());

    Resource descriptorsResource = new ClassPathResource("collections/descriptors.csv");
    MultipartFile descriptorsFile =
        new MockMultipartFile("descriptorsFile", descriptorsResource.getInputStream());

    assertDoesNotThrow(
        () ->
            getClient()
                .updateDescriptorGroup(
                    collectionKey, 1L, ExportFormat.CSV, descriptorsFile, "title", "desc", Set.of("test-tag")));
  }

  @Test
  public void getDescriptorGroupTest() {
    UUID collectionKey = UUID.randomUUID();
    DescriptorGroup descriptorGroup = new DescriptorGroup();
    descriptorGroup.setCollectionKey(collectionKey);
    descriptorGroup.setTitle("title");

    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);
    when(descriptorsService.getDescriptorGroup(1L)).thenReturn(descriptorGroup);

    assertEquals(descriptorGroup, getClient().getCollectionDescriptorGroup(collectionKey, 1L));
  }

  @Test
  public void listDescriptorGroupTest() {
    DescriptorGroup descriptorGroup = new DescriptorGroup();
    descriptorGroup.setCollectionKey(UUID.randomUUID());
    descriptorGroup.setTitle("title");

    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);
    when(descriptorsService.listDescriptorGroups(
            any(UUID.class), any(DescriptorGroupSearchRequest.class)))
        .thenReturn(new PagingResponse<>(0, 10, 1L, Collections.singletonList(descriptorGroup)));

    assertEquals(
        1,
        getClient()
            .listCollectionDescriptorGroups(
                UUID.randomUUID(), DescriptorGroupSearchRequest.builder().q("foo").build())
            .getResults()
            .size());
  }

  @Test
  public void listDescriptorsTest() {
    Descriptor descriptor = new Descriptor();
    descriptor.setDescriptorGroupKey(1L);
    descriptor.setUsageRank(Rank.ABERRATION);
    descriptor.setCountry(Country.SPAIN);

    UUID collectionKey = UUID.randomUUID();
    DescriptorGroup descriptorGroup = new DescriptorGroup();
    descriptorGroup.setCollectionKey(collectionKey);
    descriptorGroup.setTitle("title");

    when(descriptorsService.getDescriptorGroup(anyLong())).thenReturn(descriptorGroup);
    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);
    when(descriptorsService.listDescriptors(any()))
        .thenReturn(new PagingResponse<>(0, 10, 1L, Collections.singletonList(descriptor)));

    assertEquals(
        1,
        getClient()
            .listCollectionDescriptors(
                UUID.randomUUID(),
                1L,
                DescriptorSearchRequest.builder()
                    .q("foo")
                    .individualCount(Collections.singletonList("1,10"))
                    .build())
            .getResults()
            .size());
  }

  @Test
  public void getDescriptorTest() {
    UUID collectionKey = UUID.randomUUID();
    Descriptor descriptor = new Descriptor();
    descriptor.setDescriptorGroupKey(1L);
    descriptor.setUsageRank(Rank.ABERRATION);
    descriptor.setCountry(Country.SPAIN);

    DescriptorGroup descriptorGroup = new DescriptorGroup();
    descriptorGroup.setCollectionKey(collectionKey);
    descriptorGroup.setTitle("title");

    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);
    when(descriptorsService.getDescriptor(anyLong())).thenReturn(descriptor);
    when(descriptorsService.getDescriptorGroup(anyLong())).thenReturn(descriptorGroup);

    assertEquals(descriptor, getClient().getCollectionDescriptor(collectionKey, 1L, 1L));
  }

  @Test
  public void deleteDescriptorTest() {
    UUID collectionKey = UUID.randomUUID();

    DescriptorGroup descriptorGroup = new DescriptorGroup();
    descriptorGroup.setKey(1L);
    descriptorGroup.setCollectionKey(collectionKey);
    descriptorGroup.setTitle("title");

    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);
    when(descriptorsService.getDescriptorGroup(anyLong())).thenReturn(descriptorGroup);

    assertDoesNotThrow(() -> getClient().deleteCollectionDescriptorGroup(collectionKey, 1L));
  }

  @Test
  public void listForInstitutionsTest() {
    // Create test institutions with distinct keys
    UUID institution1Key = UUID.randomUUID();
    UUID institution2Key = UUID.randomUUID();

    // Create collections for these institutions
    Collection collection1 = testData.newEntity();
    collection1.setCode("C1");
    collection1.setName("Botanical Collection");
    collection1.setInstitutionKey(institution1Key);

    Collection collection2 = testData.newEntity();
    collection2.setCode("C2");
    collection2.setName("Herbarium Collection");
    collection2.setInstitutionKey(institution1Key);

    Collection collection3 = testData.newEntity();
    collection3.setCode("C3");
    collection3.setName("Zoological Collection");
    collection3.setInstitutionKey(institution2Key);

    // Create collection views
    CollectionView view1 = new CollectionView();
    view1.setCollection(collection1);

    CollectionView view2 = new CollectionView();
    view2.setCollection(collection2);

    CollectionView view3 = new CollectionView();
    view3.setCollection(collection3);

    // Setup mock to respond based on captured arguments
    List<CollectionView> institution1Collections = Arrays.asList(view1, view2);
    List<CollectionView> allCollections = Arrays.asList(view1, view2, view3);
    List<CollectionView> emptyList = Collections.emptyList();

    // Use any() for the mock, but we'll use the captor to verify the correct arguments later
    when(collectionService.getCollectionsForInstitutionsBySearch(any(InstitutionSearchRequest.class)))
        .thenAnswer(invocation -> {
            InstitutionSearchRequest request = invocation.getArgument(0);

            // Check for name search - Institution 1 collections
            if (request.getName() != null &&
                request.getName().contains("Botanical Institute")) {
                return institution1Collections;
            }

            // Check for keyword search - All collections
            if (request.getQ() != null &&
                "Collection".equals(request.getQ())) {
                return allCollections;
            }

            // Check for no match search - Empty list
            if (request.getQ() != null &&
                "Non-existent Institution".equals(request.getQ())) {
                return emptyList;
            }

            // Default case
            return emptyList;
        });

    // Scenario 1: Search by institution name
    InstitutionSearchRequest nameSearchRequest = InstitutionSearchRequest.builder()
        .name(Collections.singletonList("Botanical Institute"))
        .build();

    // Execute test and verify results
    PagingResponse<CollectionView> nameSearchResponse = getClient().listForInstitutions(nameSearchRequest);
    assertEquals(2, nameSearchResponse.getResults().size());
    assertTrue(nameSearchResponse.getResults().stream()
        .map(CollectionView::getCollection)
        .allMatch(c -> c.getInstitutionKey().equals(institution1Key)));

    // Scenario 2: Search by keyword that matches multiple institutions
    InstitutionSearchRequest keywordSearchRequest = InstitutionSearchRequest.builder()
        .q("Collection")
        .build();

    PagingResponse<CollectionView> keywordResponse = getClient().listForInstitutions(keywordSearchRequest);
    assertEquals(3, keywordResponse.getResults().size());

    // Scenario 3: Search with no matching results
    InstitutionSearchRequest noMatchRequest = InstitutionSearchRequest.builder()
        .q("Non-existent Institution")
        .build();

    PagingResponse<CollectionView> emptyResponse = getClient().listForInstitutions(noMatchRequest);
    assertEquals(0, emptyResponse.getResults().size());
  }

  @Test
  public void createDescriptorChangeSuggestionTest() throws IOException {
    UUID collectionKey = UUID.randomUUID();
    DescriptorChangeSuggestion descriptorChangeSuggestion = newDescriptorChangeSuggestion(collectionKey);
    Long suggestionKey = 1L;
    descriptorChangeSuggestion.setKey(suggestionKey);

    // Mock the entity exists check
    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);

    // Mock service methods
    when(descriptorChangeSuggestionService.createSuggestion(
        any(InputStream.class),
        anyString(),
        any(DescriptorChangeSuggestionRequest.class)))
      .thenReturn(descriptorChangeSuggestion);

    Resource descriptorsResource = new ClassPathResource("collections/descriptors.csv");
    MultipartFile descriptorsFile =
      new MockMultipartFile("file", "descriptors.csv", "text/csv", descriptorsResource.getInputStream());

    // Create the suggestion
    DescriptorChangeSuggestion created = getClient()
      .createDescriptorSuggestion(
        collectionKey,
        descriptorsFile,
        descriptorChangeSuggestion.getType(),
        descriptorChangeSuggestion.getTitle(),
        descriptorChangeSuggestion.getDescription(),
        descriptorChangeSuggestion.getFormat(),
        descriptorChangeSuggestion.getComments(),
        descriptorChangeSuggestion.getProposedBy(),
        descriptorChangeSuggestion.getTags()
      );

    // Verify create suggestion
    assertEquals(suggestionKey, created.getKey());
    assertEquals(descriptorChangeSuggestion.getTitle(), created.getTitle());
    assertEquals(descriptorChangeSuggestion.getDescription(), created.getDescription());
    assertEquals(descriptorChangeSuggestion.getFormat(), created.getFormat());
    assertEquals(descriptorChangeSuggestion.getTags(), created.getTags());
    verify(descriptorChangeSuggestionService).createSuggestion(
        any(InputStream.class),
        anyString(),
        any(DescriptorChangeSuggestionRequest.class));
  }

  @Test
  public void listAllDescriptorSuggestionsTest() {
    // Create suggestions for different collections
    UUID collectionKey1 = UUID.randomUUID();
    UUID collectionKey2 = UUID.randomUUID();

    DescriptorChangeSuggestion suggestion1 = newDescriptorChangeSuggestion(collectionKey1);
    suggestion1.setKey(1L);

    DescriptorChangeSuggestion suggestion2 = newDescriptorChangeSuggestion(collectionKey2);
    suggestion2.setKey(2L);
    suggestion2.setTitle("Second Collection Descriptors");

    List<DescriptorChangeSuggestion> allSuggestions = Arrays.asList(suggestion1, suggestion2);

    // Mock the entity exists check
    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);

    // Set up the mock for list all suggestions
    when(descriptorChangeSuggestionService.list(
        any(Pageable.class),
        isNull(),
        isNull(),
        isNull(),
        isNull() // collectionKey is null for listing all suggestions
    )).thenReturn(new PagingResponse<>(new PagingRequest(), Long.valueOf(allSuggestions.size()), allSuggestions));

    // Call the client method
    PagingResponse<DescriptorChangeSuggestion> result = getClient().listAllDescriptorSuggestions(
        null, null, null, new PagingRequest());

    // Verify results
    assertEquals(allSuggestions.size(), result.getResults().size());
    assertEquals(2, result.getCount());

    // Verify both collections are represented
    assertTrue(result.getResults().stream()
        .anyMatch(s -> s.getCollectionKey().equals(collectionKey1)));
    assertTrue(result.getResults().stream()
        .anyMatch(s -> s.getCollectionKey().equals(collectionKey2)));

    verify(descriptorChangeSuggestionService).list(
        any(Pageable.class), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  public void listDescriptorSuggestionsTest() {
    UUID collectionKey = UUID.randomUUID();
    DescriptorChangeSuggestion suggestion1 = newDescriptorChangeSuggestion(collectionKey);
    suggestion1.setKey(1L);
    suggestion1.setType(Type.CREATE);
    suggestion1.setComments(Collections.singletonList("comment 1"));
    suggestion1.setProposedBy("test1@example.com");

    DescriptorChangeSuggestion suggestion2 = newDescriptorChangeSuggestion(collectionKey);
    suggestion2.setKey(2L);
    suggestion2.setType(Type.UPDATE);
    suggestion2.setComments(Collections.singletonList("comment 2"));
    suggestion2.setProposedBy("test2@example.com");

    List<DescriptorChangeSuggestion> suggestions = Arrays.asList(suggestion1, suggestion2);

    // Mock the entity exists check
    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);

    // Set up the mock with any() for Pageable
    when(descriptorChangeSuggestionService.list(
        any(Pageable.class),
        isNull(),
        isNull(),
        isNull(),
        eq(collectionKey)
    )).thenReturn(new PagingResponse<>(new PagingRequest(), Long.valueOf(suggestions.size()), suggestions));

    // Call the client method
    PagingResponse<DescriptorChangeSuggestion> result = getClient().listDescriptorSuggestions(
        collectionKey, null, null, null, new PagingRequest());

    assertEquals(suggestions.size(), result.getResults().size());
    assertEquals(2, result.getCount());
    assertEquals(suggestion1.getType(), result.getResults().get(0).getType());
    assertEquals(suggestion2.getType(), result.getResults().get(1).getType());

    verify(descriptorChangeSuggestionService).list(
        any(Pageable.class), isNull(), isNull(), isNull(), eq(collectionKey));
  }

  @Test
  public void updateDescriptorChangeSuggestionTest() throws IOException {
    UUID collectionKey = UUID.randomUUID();
    long suggestionKey = 1;
    DescriptorChangeSuggestion descriptorChangeSuggestion = newDescriptorChangeSuggestion(collectionKey);
    descriptorChangeSuggestion.setKey(suggestionKey);

    // Mock the entity exists check
    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);

    when(descriptorChangeSuggestionService.updateSuggestion(
      eq(suggestionKey),
      any(DescriptorChangeSuggestionRequest.class),
      any(InputStream.class),
      anyString()
    )).thenReturn(descriptorChangeSuggestion);

    Resource descriptorsResource = new ClassPathResource("collections/descriptors.csv");
    MultipartFile descriptorsFile =
      new MockMultipartFile("file", "descriptors.csv", "text/csv", descriptorsResource.getInputStream());

    // Call the client
    assertDoesNotThrow( () -> getClient()
      .updateDescriptorSuggestion(
        collectionKey,
        suggestionKey,
        descriptorsFile,
        descriptorChangeSuggestion.getType(),
        descriptorChangeSuggestion.getTitle(),
        descriptorChangeSuggestion.getDescription(),
        descriptorChangeSuggestion.getFormat(),
        descriptorChangeSuggestion.getComments(),
        descriptorChangeSuggestion.getProposedBy(),
        descriptorChangeSuggestion.getTags()
      ));

    verify(descriptorChangeSuggestionService).updateSuggestion(
      eq(suggestionKey),
      any(DescriptorChangeSuggestionRequest.class),
      any(InputStream.class),
      anyString());
  }

  @Test
  public void getDescriptorSuggestionTest() {
    UUID collectionKey = UUID.randomUUID();
    long suggestionKey = 1;
    DescriptorChangeSuggestion suggestion = newDescriptorChangeSuggestion(collectionKey);
    suggestion.setKey(suggestionKey);

    // Mock the entity exists check
    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);
    when(descriptorChangeSuggestionService.getSuggestion(anyLong())).thenReturn(suggestion);

    DescriptorChangeSuggestion result = getClient().getDescriptorSuggestion(collectionKey, suggestionKey);

    Assertions.assertNotNull(result);
    assertEquals(suggestion.getKey(), result.getKey());
    assertEquals(suggestion.getType(), result.getType());
    assertEquals(suggestion.getTitle(), result.getTitle());

    verify(descriptorChangeSuggestionService).getSuggestion(suggestionKey);
  }

  @Test
  public void applyDescriptorSuggestionTest() throws IOException {
    UUID collectionKey = UUID.randomUUID();
    long suggestionKey = 1;

    // Mock the entity exists check
    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);
    doNothing().when(descriptorChangeSuggestionService).applySuggestion(suggestionKey);

    assertDoesNotThrow(() -> getClient().applyDescriptorSuggestion(collectionKey, suggestionKey));

    verify(descriptorChangeSuggestionService).applySuggestion(suggestionKey);
  }

  @Test
  public void discardDescriptorSuggestionTest() {
    UUID collectionKey = UUID.randomUUID();
    long suggestionKey = 1;

    // Mock the entity exists check
    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);
    doNothing().when(descriptorChangeSuggestionService).discardSuggestion(suggestionKey);

    assertDoesNotThrow(() -> getClient().discardDescriptorSuggestion(collectionKey, suggestionKey));

    verify(descriptorChangeSuggestionService).discardSuggestion(suggestionKey);
  }

  @Test
  public void searchDescriptorGroupByTagsTest() {
    UUID collectionKey = UUID.randomUUID();
    DescriptorGroup descriptorGroup = new DescriptorGroup();
    descriptorGroup.setKey(1L);
    descriptorGroup.setCollectionKey(collectionKey);
    descriptorGroup.setTitle("title");
    descriptorGroup.setTags(Set.of("test-tag"));

    when(resourceNotFoundService.entityExists(any(), any())).thenReturn(true);
    when(descriptorsService.getDescriptorGroup(anyLong())).thenReturn(descriptorGroup);
    when(descriptorsService.listDescriptorGroups(any(), any())).thenReturn(
        new PagingResponse<>(new PagingRequest(), 1L, Collections.singletonList(descriptorGroup)));

    DescriptorGroupSearchRequest searchRequest = DescriptorGroupSearchRequest.builder()
        .tags(Set.of("test-tag"))
        .build();

    PagingResponse<DescriptorGroup> response = getClient().listCollectionDescriptorGroups(collectionKey, searchRequest);
    assertEquals(1, response.getCount());
    assertEquals("test-tag", response.getResults().get(0).getTags().iterator().next());
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

  private DescriptorChangeSuggestion newDescriptorChangeSuggestion(UUID collectionKey) {
    DescriptorChangeSuggestion suggestion = new DescriptorChangeSuggestion();
    suggestion.setKey(1L);
    suggestion.setType(Type.CREATE);
    suggestion.setTitle("Collection Descriptors!!");
    suggestion.setDescription("Collection descriptors for specimen counts and dates");
    suggestion.setFormat(ExportFormat.CSV);
    suggestion.setComments(Arrays.asList("Initial upload of collection descriptors", "Comment required!"));
    suggestion.setProposedBy("aa@aa.org");
    suggestion.setCollectionKey(collectionKey);
    suggestion.setStatus(Status.PENDING);
    suggestion.setTags(Set.of("tag1", "tag2"));
    return suggestion;
  }
}
