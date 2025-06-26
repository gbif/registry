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
import static org.gbif.registry.cli.util.RegistryCliUtils.loadConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for VocabularyFacetUpdaterCallback that tests the actual
 * message handling workflow end-to-end with real database operations.
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

    VocabularyFacetUpdaterConfiguration config = loadConfig("vocabularyfacetupdater/vocabulary-facet-updater.yml", VocabularyFacetUpdaterConfiguration.class);
    config.setDbConfig(toDbConfig(PG_CONTAINER));

    // Create mock ConceptClient with test data
    mockConceptClient = mock(ConceptClient.class);
    setupMockConceptClient();

    ApplicationContext ctx = SpringContextBuilder.create()
        .withDbConfiguration(config.getDbConfig())
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
  }

  private static ConceptView createConceptView(Long key, String name, Long parentKey) {
    Concept concept = new Concept();
    concept.setKey(key);
    concept.setName(name);
    concept.setParentKey(parentKey);
    concept.setVocabularyKey(getVocabularyKey(name));

    ConceptView conceptView = new ConceptView();
    conceptView.setConcept(concept);
    return conceptView;
  }

  private static long getVocabularyKey(String conceptName) {
    // Map concept names to vocabulary keys based on the vocabulary they belong to
    if ("Biological".equals(conceptName) || "Archaeological".equals(conceptName) ||
        "Paleontological".equals(conceptName) || "Anthropological".equals(conceptName)) {
      return 1004L; // CollectionContentType
    } else if ("SampleDried".equals(conceptName) || "StorageIndoors".equals(conceptName) ||
               "StorageControlledAtmosphere".equals(conceptName)) {
      return 1005L; // PreservationType
    } else if ("LifeSciences".equals(conceptName) || "Botany".equals(conceptName) ||
               "Zoology".equals(conceptName)) {
      return 1001L; // Discipline
    } else if ("Museum".equals(conceptName) || "University".equals(conceptName) ||
               "Herbarium".equals(conceptName)) {
      return 1002L; // InstitutionType
    } else if ("Government".equals(conceptName) || "Academic".equals(conceptName) ||
               "Private".equals(conceptName)) {
      return 1003L; // InstitutionalGovernance
    } else if ("Institutional".equals(conceptName) || "Project".equals(conceptName)) {
      return 1006L; // AccessionStatus
    } else {
      return Math.abs(conceptName.hashCode()) % 10000L + 1000L; // Fallback
    }
  }

  @BeforeEach
  public void prepareDatabase() throws Exception {
    Connection con = PG_CONTAINER.createConnection("");

    // Clear all concept-related data to ensure test isolation
    PreparedStatement stmt = con.prepareStatement("DELETE FROM collection_concept_links");
    stmt.executeUpdate();

    stmt = con.prepareStatement("DELETE FROM institution_concept_links");
    stmt.executeUpdate();

    stmt = con.prepareStatement("DELETE FROM grscicoll_vocab_concept");
    stmt.executeUpdate();

    con.close();

    // Reset mock to default state for each test
    setupMockConceptClient();
  }

  @Test
  public void handleMessageForSupportedVocabularyShouldPopulateFacets() throws Exception {
    // Verify initial state - no facets exist
    assertFacetCount("CollectionContentType", 0);

    // Create VocabularyReleasedMessage for CollectionContentType
    VocabularyReleasedMessage message = createVocabularyReleasedMessage(
        "CollectionContentType",
        "1.0",
        "https://api.gbif-dev.org/v1/vocabularies/CollectionContentType/releases/1.0");

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
        "https://api.gbif-dev.org/v1/vocabularies/UnsupportedVocabulary/releases/1.0");

    // When: Handle the message
    callback.handleMessage(message);

    // Then: Verify no facets were created (vocabulary not in config)
    assertFacetCount("UnsupportedVocabulary", 0);
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

  @Test
  public void handleMessageForDeprecatedConceptsShouldUpdateStatus() throws Exception {
    // Given: Insert existing active concept
    long existingConceptKey = System.currentTimeMillis() % 100000L + 60000L;
    insertTestFacetWithKey("CollectionContentType", "TestConcept", "testconcept", existingConceptKey);
    assertFacetCount("CollectionContentType", 1);

    // Create a mock for CollectionContentType with deprecated concept (same key)
    PagingResponse<ConceptView> deprecatedResponse = new PagingResponse<>();
    Concept deprecatedConcept = new Concept();
    deprecatedConcept.setKey(existingConceptKey); // Use the same key as inserted concept
    deprecatedConcept.setName("TestConcept");
    deprecatedConcept.setVocabularyKey(1004L);
    deprecatedConcept.setDeprecated(java.time.ZonedDateTime.now()); // Mark as deprecated
    deprecatedConcept.setDeprecatedBy("test-user");

    ConceptView deprecatedConceptView = new ConceptView();
    deprecatedConceptView.setConcept(deprecatedConcept);

    deprecatedResponse.setResults(List.of(deprecatedConceptView));
    deprecatedResponse.setEndOfRecords(true);

    // Set up mock for this specific test
    when(mockConceptClient.listConceptsLatestRelease(eq("CollectionContentType"), any(ConceptListParams.class)))
        .thenReturn(deprecatedResponse);

    // Create a separate callback for this test
    VocabularyFacetUpdaterCallback tempCallback = new VocabularyFacetUpdaterCallback(
        vocabularyConceptService,
        Set.of("CollectionContentType"));

    // Create a mock message
    VocabularyReleasedMessage message = createMockMessage("CollectionContentType", "2.0");

    // When: Handle the message
    tempCallback.handleMessage(message);

    // Then: Verify the concept is now marked as deprecated but still exists
    assertFacetCount("CollectionContentType", 1);

    // Check that the concept is marked as deprecated
    Connection con = PG_CONTAINER.createConnection("");
    PreparedStatement stmt = con.prepareStatement(
        "SELECT deprecated, deprecated_by FROM grscicoll_vocab_concept WHERE vocabulary_name = ? AND name = ?");
    stmt.setString(1, "CollectionContentType");
    stmt.setString(2, "TestConcept");

    ResultSet rs = stmt.executeQuery();
    rs.next();
    assertNotNull(rs.getTimestamp("deprecated"), "Concept should be marked as deprecated");
    assertEquals("test-user", rs.getString("deprecated_by"), "Deprecated by should be set");

    con.close();
  }

  private void insertTestFacetWithKey(String vocabularyName, String name, String path, long conceptKey) throws Exception {
    Connection con = PG_CONTAINER.createConnection("");
    PreparedStatement stmt = con.prepareStatement(
        "INSERT INTO grscicoll_vocab_concept (concept_key, vocabulary_key, vocabulary_name, name, path) VALUES (?, ?, ?, ?, ?::ltree)");

    long vocabularyKey = getVocabularyKey(name);

    stmt.setLong(1, conceptKey);
    stmt.setLong(2, vocabularyKey);
    stmt.setString(3, vocabularyName);
    stmt.setString(4, name);
    stmt.setString(5, path);
    stmt.executeUpdate();

    con.close();
  }

  private VocabularyReleasedMessage createVocabularyReleasedMessage(String vocabularyName, String version, String downloadUrl) {
    return createMockMessage(vocabularyName, version);
  }

  private VocabularyReleasedMessage createMockMessage(String vocabularyName, String version) {
    VocabularyReleasedMessage message = mock(VocabularyReleasedMessage.class);
    when(message.getVocabularyName()).thenReturn(vocabularyName);
    when(message.getVersion()).thenReturn(version);
    when(message.getReleaseDownloadUrl()).thenReturn(URI.create("https://api.gbif-dev.org/v1/vocabularies/" + vocabularyName + "/releases/" + version));
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
