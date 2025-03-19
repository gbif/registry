package org.gbif.registry.ws.it;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.registry.metasync.cetaf.CetafCollectionResponse;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.DescriptorsService;
import org.gbif.api.vocabulary.Continent;
import org.gbif.dwc.terms.Vocabulary;
import org.gbif.registry.service.CetafSyncServiceImpl;
import org.gbif.registry.ws.client.CetafClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {RegistryIntegrationTestsConfiguration.class, CetafSyncServiceIT.TestConfig.class})
@ActiveProfiles("test")
class CetafSyncServiceIT {

    @TestConfiguration
    public static class TestConfig {
        @Bean
        @Primary
        public CetafClient cetafClient() {
            return mock(CetafClient.class);
        }
    }

    @Autowired
    private CetafSyncServiceImpl cetafSyncService;
Continent
    @Autowired
    private CollectionService collectionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CetafClient cetafClient;

    @MockBean
    private DescriptorsService descriptorsService;

    private CetafCollectionResponse loadTestResponse() throws IOException {
        ClassPathResource resource = new ClassPathResource("cetaf-response.json");
        return objectMapper.readValue(resource.getInputStream(), CetafCollectionResponse.class);
    }

    @Test
    void testUpdateCollectionFromCetaf_WithValidData() throws IOException {
        // Create test collection
        Collection collection = new Collection();
        collection.setCode("TEST-CODE");
        collection.setName("Test Collection");
        collection.setAlternativeCodes(new ArrayList<>());
        UUID collectionKey = collectionService.create(collection);

        // Load and mock CETAF API response with test data
        CetafCollectionResponse response = loadTestResponse();
        when(cetafClient.getCollectionById(anyString(), anyString(), anyString())).thenReturn(response);

        // Execute
        cetafSyncService.updateCollectionFromCetaf("TEST-INST COLLECTION", collectionKey);

        // Verify collection was updated with correct data
        Collection updatedCollection = collectionService.get(collectionKey);
        assertNotNull(updatedCollection);
        assertEquals(collectionKey, updatedCollection.getKey());
        assertEquals("COLLECTION", updatedCollection.getCode());
        assertEquals("This is a test collection description for testing purposes. Contains various specimens and samples.",
            updatedCollection.getDescription());

        // Verify alternative codes
        assertFalse(updatedCollection.getAlternativeCodes().isEmpty());
        assertTrue(updatedCollection.getAlternativeCodes().stream()
            .anyMatch(code -> code.getCode().equals("TEST-INST COLLECTION")));

        // Verify child collections
        assertFalse(updatedCollection.getIncorporatedCollections().isEmpty());
        assertTrue(updatedCollection.getIncorporatedCollections().contains("TEST-INST-MINERALS"));
        assertTrue(updatedCollection.getIncorporatedCollections().contains("TEST-INST-PETROLOGY"));
        assertTrue(updatedCollection.getIncorporatedCollections().contains("TEST-INST-SEDIMENTS"));

        // Verify contact
        assertFalse(updatedCollection.getContactPersons().isEmpty());
        assertTrue(updatedCollection.getContactPersons().stream()
            .anyMatch(contact -> contact.getEmail().contains("test.curator@example.com")));
    }

    @Test
    void testUpdateCollectionFromCetaf_WithEmptyData() {
        // Create test collection
        Collection collection = new Collection();
        collection.setCode("TEST-CODE");
        collection.setName("Test Collection");
        collection.setAlternativeCodes(new ArrayList<>());
        UUID collectionKey = collectionService.create(collection);

        // Mock CETAF API response with empty data
        CetafCollectionResponse response = new CetafCollectionResponse();
        response.setData(new ArrayList<>());
        when(cetafClient.getCollectionById(anyString(), anyString(), anyString())).thenReturn(response);

        // Execute
        cetafSyncService.updateCollectionFromCetaf("TEST-INST COLLECTION", collectionKey);

        // Verify collection remains unchanged
        Collection updatedCollection = collectionService.get(collectionKey);
        assertNotNull(updatedCollection);
        assertEquals("TEST-CODE", updatedCollection.getCode());
        assertEquals("Test Collection", updatedCollection.getName());
    }

    @Test
    void testUpdateCollectionFromCetaf_WithApiError() {
        // Create test collection
        Collection collection = new Collection();
        collection.setCode("TEST-CODE");
        collection.setName("Test Collection");
        collection.setAlternativeCodes(new ArrayList<>());
        UUID collectionKey = collectionService.create(collection);

        // Mock CETAF API error
        when(cetafClient.getCollectionById(anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("API Error"));

        // Execute and verify exception is handled
        assertDoesNotThrow(() -> cetafSyncService.updateCollectionFromCetaf("TEST-INST COLLECTION", collectionKey));

        // Verify collection remains unchanged
        Collection updatedCollection = collectionService.get(collectionKey);
        assertNotNull(updatedCollection);
        assertEquals("TEST-CODE", updatedCollection.getCode());
        assertEquals("Test Collection", updatedCollection.getName());
    }
}
