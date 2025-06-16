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
import org.gbif.registry.cli.common.spring.SpringContextBuilder;
import org.gbif.registry.database.BaseDBTest;
import org.gbif.registry.service.VocabularyConceptService;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.vocabulary.model.Concept;

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
 * Integration test for VocabularyFacetService that tests the actual
 * vocabulary facet update workflow end-to-end with real database operations
 * but mocked ConceptClient for test isolation.
 */
@DisabledIfSystemProperty(named = "test.vocabulary.facet", matches = "false")
public class VocabularyFacetUpdaterCommandIT extends BaseDBTest {

  private static VocabularyConceptService vocabularyConceptService;
  private static ApplicationContext ctx;
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
    ctx = SpringContextBuilder.create()
        .withVocabularyFacetUpdaterConfiguration(config)
        .withComponents(
            VocabularyConceptService.class,
            WithMyBatis.class,
            TestConfiguration.class)
        .build();

    vocabularyConceptService = ctx.getBean(VocabularyConceptService.class);
  }

  private static void setupMockConceptClient() {
    // Mock CollectionContentType vocabulary
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
        createConceptView(12L, "StorageControlledAtmosphere", 11L), // Child of StorageIndoors
        createConceptView(13L, "SampleCryopreserved", 10L) // Child of SampleDried
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

    // Mock non-existent vocabulary
    PagingResponse<ConceptView> emptyResponse = new PagingResponse<>();
    emptyResponse.setResults(List.of());
    emptyResponse.setEndOfRecords(true);

    when(mockConceptClient.listConceptsLatestRelease(eq("NonExistentVocabulary"), any(ConceptListParams.class)))
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
  public void testVocabularyFacetUpdate() throws Exception {
    // Verify initial state - no facets exist
    assertFacetCount("CollectionContentType", 0);
    assertFacetCount("PreservationType", 0);

    // Run the vocabulary facet updater for CollectionContentType
    vocabularyConceptService.populateConceptsForVocabulary("CollectionContentType");

    // Verify expected number of facets were created (4 concepts in mock)
    assertFacetCount("CollectionContentType", 4);

    // Run for PreservationType
    vocabularyConceptService.populateConceptsForVocabulary("PreservationType");

    // Verify expected number of facets were created (4 concepts in mock)
    assertFacetCount("PreservationType", 4);

    // Verify hierarchical structure is maintained for CollectionContentType
    assertHierarchicalStructure("CollectionContentType");

    // Verify specific facets exist
    assertFacetExists("CollectionContentType", "Biological");
    assertFacetExists("CollectionContentType", "Paleontological");
    assertFacetExists("PreservationType", "SampleDried");
    assertFacetExists("PreservationType", "StorageControlledAtmosphere");
  }

  @Test
  public void testVocabularyFacetUpdateWithExistingData() throws Exception {
    // Insert some existing facets
    insertTestFacet("CollectionContentType", "ExistingType", "ExistingType");

    int initialCount = getFacetCount("CollectionContentType");
    assertEquals(1, initialCount, "Should have 1 existing facet");

    // Run the updater (this should replace existing facets)
    vocabularyConceptService.populateConceptsForVocabulary("CollectionContentType");

    // Verify mock data replaced the existing facet
    assertFacetCount("CollectionContentType", 4); // Should have 4 mock concepts
    assertFacetExists("CollectionContentType", "Biological");
    assertFacetNotExists("CollectionContentType", "ExistingType"); // Old facet should be gone
  }

  @Test
  public void testNonExistentVocabulary() throws Exception {
    // Test with non-existent vocabulary - should not crash
    assertDoesNotThrow(() -> {
      vocabularyConceptService.populateConceptsForVocabulary("NonExistentVocabulary");
    });

    // Should not create any facets
    assertFacetCount("NonExistentVocabulary", 0);
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

  private void assertFacetNotExists(String vocabularyName, String facetName) throws Exception {
    Connection con = PG_CONTAINER.createConnection("");
    PreparedStatement stmt = con.prepareStatement(
        "SELECT COUNT(*) FROM grscicoll_vocab_concept WHERE vocabulary_name = ? AND name = ?");
    stmt.setString(1, vocabularyName);
    stmt.setString(2, facetName);

    ResultSet rs = stmt.executeQuery();
    rs.next();
    int count = rs.getInt(1);

    con.close();
    assertEquals(0, count, "Facet '" + facetName + "' should not exist for vocabulary " + vocabularyName);
  }
}
