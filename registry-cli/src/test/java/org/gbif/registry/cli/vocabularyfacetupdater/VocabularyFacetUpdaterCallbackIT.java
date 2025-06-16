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
package org.gbif.registry.cli.vocabularyfacetupdater;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.common.messaging.api.messages.VocabularyReleasedMessage;
import org.gbif.registry.cli.common.spring.SpringContextBuilder;
import org.gbif.registry.database.BaseDBTest;
import org.gbif.registry.service.VocabularyConceptService;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.vocabulary.model.Concept;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.gbif.registry.cli.util.EmbeddedPostgresTestUtils.toDbConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for VocabularyFacetUpdaterCallback that tests the actual
 * message handling workflow end-to-end with real database operations.
 * Similar to DoiUpdaterListenerIT pattern.
 */
@DisabledIfSystemProperty(named = "test.vocabulary.facet", matches = "false")
public class VocabularyFacetUpdaterCallbackIT extends BaseDBTest {

  private static VocabularyFacetUpdaterCallback callback;
  private static VocabularyConceptService vocabularyConceptService;
  private static ConceptClient mockConceptClient;

  @Configuration
  static class TestConfiguration {
    @Bean
    @Primary
    public ConceptClient conceptClient() {
      return mockConceptClient;
    }
  }

  @BeforeAll
  public static void beforeAll() throws Exception {
    // Create mock ConceptClient with test data
    mockConceptClient = mock(ConceptClient.class);
    setupMockConceptClient();

    VocabularyFacetUpdaterConfiguration config = new VocabularyFacetUpdaterConfiguration();
    config.setDbConfig(toDbConfig(PG_CONTAINER));
    config.apiRootUrl = "http://test-api.example.com/v1/"; // Mock URL
    config.vocabulariesToProcess = Set.of("Discipline", "InstitutionType", "InstitutionalGovernance", "CollectionContentType", "PreservationType", "AccessionStatus");

    // Build the Spring context with mock ConceptClient
    ApplicationContext ctx = SpringContextBuilder.create()
        .withVocabularyFacetUpdaterConfiguration(config)
        .withComponents(
            VocabularyConceptService.class,
            WithMyBatis.class,
            TestConfiguration.class)
        .build();

    vocabularyConceptService = ctx.getBean(VocabularyConceptService.class);

    // Create the callback with the service and configuration
    callback = new VocabularyFacetUpdaterCallback(
        vocabularyConceptService,
        config.vocabulariesToProcess);
  }

  private static void setupMockConceptClient() {
    // Mock CollectionContentType vocabulary with hierarchical data
    PagingResponse<ConceptView> collectionContentResponse = new PagingResponse<>();
    collectionContentResponse.setResults(List.of(
        createConceptView(1L, "Biological", null),
        createConceptView(2L, "Archaeological", null),
        createConceptView(3L, "Paleontological", 1L), // Child of Biological
        createConceptView(4L, "Anthropological", 2L) // Child of Archaeological
    ));
    collectionContentResponse.setEndOfRecords(true);

    when(mockConceptClient.listConceptsLatestRelease(eq("CollectionContentType"), any(ConceptListParams.class)))
        .thenReturn(collectionContentResponse);

    // Mock PreservationType vocabulary
    PagingResponse<ConceptView> preservationTypeResponse = new PagingResponse<>();
    preservationTypeResponse.setResults(List.of(
        createConceptView(10L, "SampleDried", null),
        createConceptView(11L, "StorageIndoors", null),
        createConceptView(12L, "StorageControlledAtmosphere", 11L) // Child of StorageIndoors
    ));
    preservationTypeResponse.setEndOfRecords(true);

    when(mockConceptClient.listConceptsLatestRelease(eq("PreservationType"), any(ConceptListParams.class)))
        .thenReturn(preservationTypeResponse);

    // Mock Discipline vocabulary
    PagingResponse<ConceptView> disciplineResponse = new PagingResponse<>();
    disciplineResponse.setResults(List.of(
        createConceptView(30L, "LifeSciences", null),
        createConceptView(31L, "Botany", 30L), // Child of LifeSciences
        createConceptView(32L, "Zoology", 30L) // Child of LifeSciences
    ));
    disciplineResponse.setEndOfRecords(true);

    when(mockConceptClient.listConceptsLatestRelease(eq("Discipline"), any(ConceptListParams.class)))
        .thenReturn(disciplineResponse);

    // Mock InstitutionType vocabulary
    PagingResponse<ConceptView> institutionTypeResponse = new PagingResponse<>();
    institutionTypeResponse.setResults(List.of(
        createConceptView(40L, "Museum", null),
        createConceptView(41L, "University", null),
        createConceptView(42L, "Herbarium", 40L) // Child of Museum
    ));
    institutionTypeResponse.setEndOfRecords(true);

    when(mockConceptClient.listConceptsLatestRelease(eq("InstitutionType"), any(ConceptListParams.class)))
        .thenReturn(institutionTypeResponse);

    // Mock InstitutionalGovernance vocabulary
    PagingResponse<ConceptView> institutionalGovernanceResponse = new PagingResponse<>();
    institutionalGovernanceResponse.setResults(List.of(
        createConceptView(50L, "Government", null),
        createConceptView(51L, "Academic", null),
        createConceptView(52L, "Private", null)
    ));
    institutionalGovernanceResponse.setEndOfRecords(true);

    when(mockConceptClient.listConceptsLatestRelease(eq("InstitutionalGovernance"), any(ConceptListParams.class)))
        .thenReturn(institutionalGovernanceResponse);

    // Mock AccessionStatus vocabulary
    PagingResponse<ConceptView> accessionStatusResponse = new PagingResponse<>();
    accessionStatusResponse.setResults(List.of(
        createConceptView(60L, "Institutional", null),
        createConceptView(61L, "Project", null),
        createConceptView(62L, "Private", null)
    ));
    accessionStatusResponse.setEndOfRecords(true);

    when(mockConceptClient.listConceptsLatestRelease(eq("AccessionStatus"), any(ConceptListParams.class)))
        .thenReturn(accessionStatusResponse);

    // Mock UnsupportedVocabulary (not in config.vocabulariesToProcess)
    PagingResponse<ConceptView> unsupportedResponse = new PagingResponse<>();
    unsupportedResponse.setResults(List.of(
        createConceptView(20L, "UnsupportedTerm", null)
    ));
    unsupportedResponse.setEndOfRecords(true);

    when(mockConceptClient.listConceptsLatestRelease(eq("UnsupportedVocabulary"), any(ConceptListParams.class)))
        .thenReturn(unsupportedResponse);

    // Mock empty vocabulary
    PagingResponse<ConceptView> emptyResponse = new PagingResponse<>();
    emptyResponse.setResults(List.of());
    emptyResponse.setEndOfRecords(true);

    when(mockConceptClient.listConceptsLatestRelease(eq("EmptyVocabulary"), any(ConceptListParams.class)))
        .thenReturn(emptyResponse);
  }

  private static ConceptView createConceptView(Long key, String name, Long parentKey) {
    Concept concept = new Concept();
    concept.setKey(key);
    concept.setName(name);
    concept.setParentKey(parentKey);

    ConceptView conceptView = new ConceptView();
    conceptView.setConcept(concept);
    return conceptView;
  }

  @BeforeEach
  public void prepareDatabase() throws Exception {
    Connection con = PG_CONTAINER.createConnection("");

    // Clear grscicoll_vocab_concept table
    PreparedStatement stmt = con.prepareStatement("DELETE FROM grscicoll_vocab_concept");
    stmt.executeUpdate();

    con.close();
  }

  @Test
  public void handleMessageForSupportedVocabularyShouldPopulateFacets() throws Exception {
    // Verify initial state - no facets exist
    assertFacetCount("CollectionContentType", 0);

    // Create VocabularyReleasedMessage for CollectionContentType
    VocabularyReleasedMessage message = createVocabularyReleasedMessage(
        "CollectionContentType",
        "1.0",
        "https://api.gbif.org/v1/vocabularies/CollectionContentType/releases/1.0");

    // When: Handle the message
    callback.handleMessage(message);

    // Then: Verify facets were created
    assertFacetCount("CollectionContentType", 4); // 4 concepts in mock
    assertFacetExists("CollectionContentType", "Biological");
    assertFacetExists("CollectionContentType", "Paleontological");
    assertHierarchicalStructure("CollectionContentType");
  }

  @Test
  public void handleMessageForUnsupportedVocabularyShouldBeSkipped() throws Exception {
    // Verify initial state - no facets exist
    assertFacetCount("UnsupportedVocabulary", 0);

    // Create VocabularyReleasedMessage for unsupported vocabulary
    VocabularyReleasedMessage message = createVocabularyReleasedMessage(
        "UnsupportedVocabulary",
        "1.0",
        "https://api.gbif.org/v1/vocabularies/UnsupportedVocabulary/releases/1.0");

    // When: Handle the message
    callback.handleMessage(message);

    // Then: Verify no facets were created (vocabulary not in config)
    assertFacetCount("UnsupportedVocabulary", 0);
  }

  @Test
  public void handleMessageForEmptyVocabularyShouldClearExistingFacets() throws Exception {
    // Given: Insert existing facets
    insertTestFacet("EmptyVocabulary", "OldConcept", "oldconcept");
    assertFacetCount("EmptyVocabulary", 1);

    // Create a separate callback for empty vocabulary (add to config temporarily)
    VocabularyFacetUpdaterCallback tempCallback = new VocabularyFacetUpdaterCallback(
        vocabularyConceptService,
        Set.of("CollectionContentType", "PreservationType", "EmptyVocabulary"));

    // Create a mock message for empty vocabulary
    VocabularyReleasedMessage message = mock(VocabularyReleasedMessage.class);
    when(message.getVocabularyName()).thenReturn("EmptyVocabulary");
    when(message.getVersion()).thenReturn("2.0");
    when(message.getReleaseDownloadUrl()).thenReturn(URI.create("https://api.gbif.org/v1/vocabularies/EmptyVocabulary/releases/2.0"));

    // When: Handle the message
    tempCallback.handleMessage(message);

    // Then: Verify existing facets were cleared
    assertFacetCount("EmptyVocabulary", 0);
  }

  @Test
  public void handleMessageForMultipleVocabulariesShouldWorkIndependently() throws Exception {
    // Verify initial state
    assertFacetCount("CollectionContentType", 0);
    assertFacetCount("PreservationType", 0);

    // Handle CollectionContentType message
    VocabularyReleasedMessage collectionMessage = createMockMessage("CollectionContentType", "1.0");
    callback.handleMessage(collectionMessage);

    // Handle PreservationType message
    VocabularyReleasedMessage preservationMessage = createMockMessage("PreservationType", "2.0");
    callback.handleMessage(preservationMessage);

    // Verify both vocabularies were processed independently
    assertFacetCount("CollectionContentType", 4);
    assertFacetCount("PreservationType", 3);
    assertFacetExists("CollectionContentType", "Biological");
    assertFacetExists("PreservationType", "SampleDried");
  }

  private VocabularyReleasedMessage createVocabularyReleasedMessage(String vocabularyName, String version, String downloadUrl) {
    return createMockMessage(vocabularyName, version);
  }

  private VocabularyReleasedMessage createMockMessage(String vocabularyName, String version) {
    VocabularyReleasedMessage message = mock(VocabularyReleasedMessage.class);
    when(message.getVocabularyName()).thenReturn(vocabularyName);
    when(message.getVersion()).thenReturn(version);
    when(message.getReleaseDownloadUrl()).thenReturn(URI.create("https://api.gbif.org/v1/vocabularies/" + vocabularyName + "/releases/" + version));
    return message;
  }

  private void assertFacetCount(String vocabularyName, int expectedCount) throws Exception {
    assertEquals(expectedCount, getFacetCount(vocabularyName),
        "Expected " + expectedCount + " facets for vocabulary " + vocabularyName);
  }

  private int getFacetCount(String vocabularyName) throws Exception {
    Connection con = PG_CONTAINER.createConnection("");
    PreparedStatement stmt = con.prepareStatement(
        "SELECT COUNT(*) FROM grscicoll_vocab_concept WHERE vocabulary_name = ?");
    stmt.setString(1, vocabularyName);

    ResultSet rs = stmt.executeQuery();
    rs.next();
    int count = rs.getInt(1);

    con.close();
    return count;
  }

  private void insertTestFacet(String vocabularyName, String name, String path) throws Exception {
    Connection con = PG_CONTAINER.createConnection("");
    PreparedStatement stmt = con.prepareStatement(
        "INSERT INTO grscicoll_vocab_concept (vocabulary_name, name, path) VALUES (?, ?, ?::ltree)");
    stmt.setString(1, vocabularyName);
    stmt.setString(2, name);
    stmt.setString(3, path);
    stmt.executeUpdate();

    con.close();
  }

  private void assertFacetExists(String vocabularyName, String facetName) throws Exception {
    Connection con = PG_CONTAINER.createConnection("");
    PreparedStatement stmt = con.prepareStatement(
        "SELECT COUNT(*) FROM grscicoll_vocab_concept WHERE vocabulary_name = ? AND name = ?");
    stmt.setString(1, vocabularyName);
    stmt.setString(2, facetName);

    ResultSet rs = stmt.executeQuery();
    rs.next();
    int count = rs.getInt(1);

    con.close();
    assertTrue(count > 0, "Facet '" + facetName + "' should exist for vocabulary " + vocabularyName);
  }

  private void assertHierarchicalStructure(String vocabularyName) throws Exception {
    Connection con = PG_CONTAINER.createConnection("");

    // Verify that hierarchical paths exist
    PreparedStatement stmt = con.prepareStatement(
        "SELECT COUNT(*) FROM grscicoll_vocab_concept WHERE vocabulary_name = ? AND nlevel(path) > 1");
    stmt.setString(1, vocabularyName);

    ResultSet rs = stmt.executeQuery();
    rs.next();
    int hierarchicalCount = rs.getInt(1);

    // For CollectionContentType, we expect hierarchical concepts
    if ("CollectionContentType".equals(vocabularyName)) {
      assertTrue(hierarchicalCount > 0,
          "Should have hierarchical facets for " + vocabularyName);
    }

    con.close();
  }
}
