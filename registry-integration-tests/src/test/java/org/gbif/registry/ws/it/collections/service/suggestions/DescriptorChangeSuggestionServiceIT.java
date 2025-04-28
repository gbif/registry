package org.gbif.registry.ws.it.collections.service.suggestions;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Collections;
import java.io.InputStream;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.descriptors.DescriptorChangeSuggestion;
import org.gbif.api.model.collections.descriptors.DescriptorChangeSuggestionRequest;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.DescriptorChangeSuggestionService;
import org.gbif.api.service.collections.DescriptorsService;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.ws.it.collections.service.BaseServiceIT;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

/** Tests the {@link DescriptorChangeSuggestionService}. */
class DescriptorChangeSuggestionServiceIT extends BaseServiceIT {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  private final DescriptorChangeSuggestionService descriptorChangeSuggestionService;
  private final CollectionService collectionService;
  private final DescriptorsService descriptorsService;

  @Autowired
  public DescriptorChangeSuggestionServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      DescriptorChangeSuggestionService descriptorChangeSuggestionService,
      CollectionService collectionService,
      DescriptorsService descriptorsService) {
    super(simplePrincipalProvider);
    this.descriptorChangeSuggestionService = descriptorChangeSuggestionService;
    this.collectionService = collectionService;
    this.descriptorsService = descriptorsService;
  }

  @Test
  void createDescriptorSuggestionTest() throws Exception {
    // State
    Collection collection = new Collection();
    collection.setCode("c1");
    collection.setName("n1");
    collectionService.create(collection);

    Resource descriptorsFile = new ClassPathResource("collections/descriptors.csv");
    DescriptorChangeSuggestionRequest request = new DescriptorChangeSuggestionRequest();
    request.setCollectionKey(collection.getKey());
    request.setTitle("Test Descriptor Set");
    request.setDescription("Test Description");
    request.setFormat(ExportFormat.CSV);
    request.setType(Type.CREATE);
    request.setProposerEmail("test@gbif.org");
    request.setComments(Collections.singletonList("Test comment"));

    // When
    DescriptorChangeSuggestion suggestion = descriptorChangeSuggestionService.createSuggestion(
        descriptorsFile.getInputStream(),
        "descriptors.csv",
        request);

    // Then
    assertNotNull(suggestion);
    assertEquals(Status.PENDING, suggestion.getStatus());
    assertEquals(Type.CREATE, suggestion.getType());
    assertEquals(collection.getKey(), suggestion.getCollectionKey());
    assertEquals("Test Descriptor Set", suggestion.getTitle());
    assertEquals("Test Description", suggestion.getDescription());
    assertEquals(ExportFormat.CSV, suggestion.getFormat());
    assertEquals(1, suggestion.getComments().size());
    assertEquals("Test comment", suggestion.getComments().get(0));
  }

  @Test
  void updateDescriptorSuggestionTest() throws Exception {
    // State
    Collection collection = new Collection();
    collection.setCode("c1");
    collection.setName("n1");
    collectionService.create(collection);

    Resource descriptorsFile = new ClassPathResource("collections/descriptors.csv");
    DescriptorChangeSuggestionRequest request = new DescriptorChangeSuggestionRequest();
    request.setCollectionKey(collection.getKey());
    request.setTitle("Test Descriptor Set");
    request.setDescription("Test Description");
    request.setFormat(ExportFormat.CSV);
    request.setType(Type.CREATE);
    request.setProposerEmail("test@gbif.org");
    request.setComments(Collections.singletonList("Test comment"));

    DescriptorChangeSuggestion suggestion = descriptorChangeSuggestionService.createSuggestion(
        descriptorsFile.getInputStream(),
        "descriptors.csv",
        request);

    // When - update the suggestion
    DescriptorChangeSuggestionRequest updateRequest = new DescriptorChangeSuggestionRequest();
    updateRequest.setCollectionKey(collection.getKey());
    updateRequest.setTitle("Updated Title");
    updateRequest.setDescription("Updated Description");
    updateRequest.setFormat(ExportFormat.CSV);
    updateRequest.setComments(Collections.singletonList("Updated comment"));

    DescriptorChangeSuggestion updatedSuggestion = descriptorChangeSuggestionService.updateSuggestion(
        suggestion.getKey(),
        updateRequest,
        descriptorsFile.getInputStream(),
        "descriptors.csv");

    // Then
    assertNotNull(updatedSuggestion);
    assertEquals(Status.PENDING, updatedSuggestion.getStatus());
    assertEquals(Type.CREATE, updatedSuggestion.getType());
    assertEquals(collection.getKey(), updatedSuggestion.getCollectionKey());
    assertEquals("Updated Title", updatedSuggestion.getTitle());
    assertEquals("Updated Description", updatedSuggestion.getDescription());
    assertEquals(ExportFormat.CSV, updatedSuggestion.getFormat());
    assertEquals(1, updatedSuggestion.getComments().size());
    assertEquals("Updated comment", updatedSuggestion.getComments().get(0));
  }

  @Test
  public void applyDescriptorSuggestionTest() throws Exception {
    // State
    Collection collection = new Collection();
    collection.setCode("c1");
    collection.setName("n1");
    collectionService.create(collection);

    Resource descriptorsFile = new ClassPathResource("collections/descriptors.csv");
    DescriptorChangeSuggestionRequest request = new DescriptorChangeSuggestionRequest();
    request.setCollectionKey(collection.getKey());
    request.setTitle("Test Descriptor Set");
    request.setDescription("Test Description");
    request.setFormat(ExportFormat.CSV);
    request.setType(Type.CREATE);
    request.setProposerEmail("test@gbif.org");
    request.setComments(Collections.singletonList("Test comment"));

    DescriptorChangeSuggestion suggestion = descriptorChangeSuggestionService.createSuggestion(
        descriptorsFile.getInputStream(),
        "descriptors.csv",
        request);

    // When
    descriptorChangeSuggestionService.applySuggestion(suggestion.getKey());

    // Then
    suggestion = descriptorChangeSuggestionService.getSuggestion(suggestion.getKey());
    assertEquals(Status.APPLIED, suggestion.getStatus());
    assertNotNull(suggestion.getApplied());
    assertNotNull(suggestion.getAppliedBy());

    // Verify the descriptor group was created
    assertNotNull(descriptorsService.getDescriptorGroup(suggestion.getDescriptorGroupKey()));
  }

  @Test
  void discardDescriptorSuggestionTest() throws Exception {
    // State
    Collection collection = new Collection();
    collection.setCode("c1");
    collection.setName("n1");
    collectionService.create(collection);

    Resource descriptorsFile = new ClassPathResource("collections/descriptors.csv");
    DescriptorChangeSuggestionRequest request = new DescriptorChangeSuggestionRequest();
    request.setCollectionKey(collection.getKey());
    request.setTitle("Test Descriptor Set");
    request.setDescription("Test Description");
    request.setFormat(ExportFormat.CSV);
    request.setType(Type.CREATE);
    request.setProposerEmail("test@gbif.org");
    request.setComments(Collections.singletonList("Test comment"));

    DescriptorChangeSuggestion suggestion = descriptorChangeSuggestionService.createSuggestion(
        descriptorsFile.getInputStream(),
        "descriptors.csv",
        request);

    // When
    descriptorChangeSuggestionService.discardSuggestion(suggestion.getKey());

    // Then
    suggestion = descriptorChangeSuggestionService.getSuggestion(suggestion.getKey());
    assertEquals(Status.DISCARDED, suggestion.getStatus());
    assertNotNull(suggestion.getDiscarded());
    assertNotNull(suggestion.getDiscardedBy());
  }

  @Test
  void listDescriptorSuggestionsTest() throws Exception {
    // State
    Collection collection = new Collection();
    collection.setCode("c1");
    collection.setName("n1");
    collectionService.create(collection);

    Resource descriptorsFile = new ClassPathResource("collections/descriptors.csv");
    DescriptorChangeSuggestionRequest request = new DescriptorChangeSuggestionRequest();
    request.setCollectionKey(collection.getKey());
    request.setTitle("Test Descriptor Set");
    request.setDescription("Test Description");
    request.setFormat(ExportFormat.CSV);
    request.setType(Type.CREATE);
    request.setProposerEmail("test@gbif.org");
    request.setComments(Collections.singletonList("Test comment"));

    DescriptorChangeSuggestion suggestion = descriptorChangeSuggestionService.createSuggestion(
        descriptorsFile.getInputStream(),
        "descriptors.csv",
        request);

    // When
    PagingResponse<DescriptorChangeSuggestion> results =
        descriptorChangeSuggestionService.list(
            new PagingRequest(0, 20), null, null, null, collection.getKey());

    // Then
    assertEquals(1, results.getResults().size());
    assertEquals(1, results.getCount());
    assertEquals(suggestion.getKey(), results.getResults().get(0).getKey());
  }

  @Test
  void getSuggestionFileTest() throws Exception {
    // State
    Collection collection = new Collection();
    collection.setCode("c1");
    collection.setName("n1");
    collectionService.create(collection);

    Resource descriptorsFile = new ClassPathResource("collections/descriptors.csv");
    DescriptorChangeSuggestionRequest request = new DescriptorChangeSuggestionRequest();
    request.setCollectionKey(collection.getKey());
    request.setTitle("Test Descriptor Set");
    request.setDescription("Test Description");
    request.setFormat(ExportFormat.CSV);
    request.setType(Type.CREATE);
    request.setProposerEmail("test@gbif.org");
    request.setComments(Collections.singletonList("Test comment"));

    DescriptorChangeSuggestion suggestion = descriptorChangeSuggestionService.createSuggestion(
        descriptorsFile.getInputStream(),
        "descriptors.csv",
        request);

    // When - get the suggestion file
    InputStream fileStream = descriptorChangeSuggestionService.getSuggestionFile(suggestion.getKey());

    // Then - verify the file content matches the original
    byte[] originalContent = StreamUtils.copyToByteArray(descriptorsFile.getInputStream());
    byte[] retrievedContent = StreamUtils.copyToByteArray(fileStream);
    assertArrayEquals(originalContent, retrievedContent, "File content should match the original");
  }

  @Test
  void getSuggestionFileNotFoundTest() throws IOException {
    // When - try to get a non-existent suggestion file
    assertNull(descriptorChangeSuggestionService.getSuggestionFile(9999L),
      "Should return null for non-existent suggestion");
  }

  @Test
  void discardSuggestionTest() throws Exception {
    // State
    Collection collection = new Collection();
    collection.setCode("c1");
    collection.setName("n1");
    collectionService.create(collection);

    Resource descriptorsFile = new ClassPathResource("collections/descriptors.csv");
    DescriptorChangeSuggestionRequest request = new DescriptorChangeSuggestionRequest();
    request.setCollectionKey(collection.getKey());
    request.setTitle("Test Descriptor Set");
    request.setDescription("Test Description");
    request.setFormat(ExportFormat.CSV);
    request.setType(Type.CREATE);
    request.setProposerEmail("test@gbif.org");
    request.setComments(Collections.singletonList("Test comment"));

    DescriptorChangeSuggestion suggestion = descriptorChangeSuggestionService.createSuggestion(
        descriptorsFile.getInputStream(),
        "descriptors.csv",
        request);

    // When - discard the suggestion
    descriptorChangeSuggestionService.discardSuggestion(suggestion.getKey());

    // Then - verify the suggestion is marked as discarded
    DescriptorChangeSuggestion discardedSuggestion =
        descriptorChangeSuggestionService.getSuggestion(suggestion.getKey());

    assertNotNull(discardedSuggestion, "Suggestion should not be null after being discarded");
    assertEquals(Status.DISCARDED, discardedSuggestion.getStatus(),
        "Suggestion status should be DISCARDED");
    assertNotNull(discardedSuggestion.getDiscarded(), "Discarded date should be set");
    assertNotNull(discardedSuggestion.getDiscardedBy(), "DiscardedBy field should be set");
  }

  @Test
  void listAllDescriptorSuggestionsTest() throws Exception {
    // State - create two collections with suggestions
    Collection collection1 = new Collection();
    collection1.setCode("c1");
    collection1.setName("n1");
    collectionService.create(collection1);

    Collection collection2 = new Collection();
    collection2.setCode("c2");
    collection2.setName("n2");
    collectionService.create(collection2);

    Resource descriptorsFile = new ClassPathResource("collections/descriptors.csv");

    // Create suggestion for collection 1
    DescriptorChangeSuggestionRequest request1 = new DescriptorChangeSuggestionRequest();
    request1.setCollectionKey(collection1.getKey());
    request1.setTitle("Test Descriptor Set 1");
    request1.setDescription("Test Description 1");
    request1.setFormat(ExportFormat.CSV);
    request1.setType(Type.CREATE);
    request1.setProposerEmail("test1@gbif.org");
    request1.setComments(Collections.singletonList("Test comment 1"));

    DescriptorChangeSuggestion suggestion1 = descriptorChangeSuggestionService.createSuggestion(
        descriptorsFile.getInputStream(),
        "descriptors1.csv",
        request1);

    // Create suggestion for collection 2
    DescriptorChangeSuggestionRequest request2 = new DescriptorChangeSuggestionRequest();
    request2.setCollectionKey(collection2.getKey());
    request2.setTitle("Test Descriptor Set 2");
    request2.setDescription("Test Description 2");
    request2.setFormat(ExportFormat.CSV);
    request2.setType(Type.UPDATE);
    request2.setProposerEmail("test2@gbif.org");
    request2.setComments(Collections.singletonList("Test comment 2"));

    DescriptorChangeSuggestion suggestion2 = descriptorChangeSuggestionService.createSuggestion(
        descriptorsFile.getInputStream(),
        "descriptors2.csv",
        request2);

    // When - list all suggestions regardless of collection
    PagingResponse<DescriptorChangeSuggestion> results =
        descriptorChangeSuggestionService.list(
            new PagingRequest(0, 20), null, null, null, null); // collectionKey is null

    // Then - should return both suggestions
    assertEquals(2, results.getResults().size(), "Should return two suggestions");
    assertEquals(2, results.getCount(), "Total count should be 2");

    // Verify both suggestions are in the results
    boolean foundSuggestion1 = false;
    boolean foundSuggestion2 = false;

    for (DescriptorChangeSuggestion suggestion : results.getResults()) {
      if (suggestion.getKey() == suggestion1.getKey()) {
        foundSuggestion1 = true;
        assertEquals(collection1.getKey(), suggestion.getCollectionKey());
        assertEquals(Type.CREATE, suggestion.getType());
      } else if (suggestion.getKey() == suggestion2.getKey()) {
        foundSuggestion2 = true;
        assertEquals(collection2.getKey(), suggestion.getCollectionKey());
        assertEquals(Type.UPDATE, suggestion.getType());
      }
    }

    assertTrue(foundSuggestion1, "Should find suggestion 1 in results");
    assertTrue(foundSuggestion2, "Should find suggestion 2 in results");
  }

  @Test
  void countTest() throws Exception {
    // State - create two collections with suggestions
    Collection collection1 = new Collection();
    collection1.setCode("c1");
    collection1.setName("n1");
    collectionService.create(collection1);

    Collection collection2 = new Collection();
    collection2.setCode("c2");
    collection2.setName("n2");
    collectionService.create(collection2);

    Resource descriptorsFile = new ClassPathResource("collections/descriptors.csv");

    // Create suggestion for collection 1 with CREATE type
    DescriptorChangeSuggestionRequest request1 = new DescriptorChangeSuggestionRequest();
    request1.setCollectionKey(collection1.getKey());
    request1.setTitle("Test Descriptor Set 1");
    request1.setDescription("Test Description 1");
    request1.setFormat(ExportFormat.CSV);
    request1.setType(Type.CREATE);
    request1.setProposerEmail("test1@gbif.org");
    request1.setComments(Collections.singletonList("Test comment 1"));

    descriptorChangeSuggestionService.createSuggestion(
        descriptorsFile.getInputStream(),
        "descriptors1.csv",
        request1);

    // Create suggestion for collection 2 with UPDATE type
    DescriptorChangeSuggestionRequest request2 = new DescriptorChangeSuggestionRequest();
    request2.setCollectionKey(collection2.getKey());
    request2.setTitle("Test Descriptor Set 2");
    request2.setDescription("Test Description 2");
    request2.setFormat(ExportFormat.CSV);
    request2.setType(Type.UPDATE);
    request2.setProposerEmail("test2@gbif.org");
    request2.setComments(Collections.singletonList("Test comment 2"));

    descriptorChangeSuggestionService.createSuggestion(
        descriptorsFile.getInputStream(),
        "descriptors2.csv",
        request2);

    // When & Then - count by different parameters
    // Count all suggestions
    assertEquals(2, descriptorChangeSuggestionService.count(null, null, null, null),
        "Count of all suggestions should be 2");

    // Count by collection
    assertEquals(1, descriptorChangeSuggestionService.count(null, null, null, collection1.getKey()),
        "Count for collection1 should be 1");

    // Count by type
    assertEquals(1, descriptorChangeSuggestionService.count(null, Type.CREATE, null, null),
        "Count for CREATE type should be 1");
    assertEquals(1, descriptorChangeSuggestionService.count(null, Type.UPDATE, null, null),
        "Count for UPDATE type should be 1");

    // Count by status (all are PENDING by default)
    assertEquals(2, descriptorChangeSuggestionService.count(Status.PENDING, null, null, null),
        "Count for PENDING status should be 2");
    assertEquals(0, descriptorChangeSuggestionService.count(Status.DISCARDED, null, null, null),
        "Count for DISCARDED status should be 0");

    // Count by proposer email
    assertEquals(1, descriptorChangeSuggestionService.count(null, null, "test1@gbif.org", null),
        "Count for proposer test1@gbif.org should be 1");
  }
}
