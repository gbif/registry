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
package org.gbif.registry.service;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.registry.persistence.mapper.GrScicollVocabConceptMapper;
import org.gbif.registry.persistence.mapper.dto.GrSciCollVocabConceptDto;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.vocabulary.model.Concept;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class VocabularyConceptServiceTest {

    @Mock
    private GrScicollVocabConceptMapper grScicollVocabConceptMapper;

    @Mock
    private ConceptClient conceptClient;

    @InjectMocks
    private VocabularyConceptService vocabularyConceptService;

    @Captor
    private ArgumentCaptor<GrSciCollVocabConceptDto> conceptDtoCaptor;

    @Test
    public void testPopulateConceptsEmptyVocabulary() throws Exception {
        String vocabularyName = "emptyVocab";

        PagingResponse<ConceptView> emptyResponse = new PagingResponse<>();
        emptyResponse.setResults(Collections.emptyList());
        emptyResponse.setEndOfRecords(true);

        when(conceptClient.listConceptsLatestRelease(anyString(), any(ConceptListParams.class)))
            .thenReturn(emptyResponse);

        vocabularyConceptService.populateConceptsForVocabulary(vocabularyName);

        // Verify no concepts were processed
        verify(grScicollVocabConceptMapper, never()).create(any(GrSciCollVocabConceptDto.class));
    }

    private ConceptView createConceptView(Long key, String name, Long parentKey) {
        Concept concept = new Concept();
        concept.setKey(key);
        concept.setName(name);
        concept.setParentKey(parentKey);
        // Skipping label creation as it's not directly used in ltree path generation
        // and its exact structure is causing issues without full model definitions.

        ConceptView conceptView = new ConceptView();
        conceptView.setConcept(concept);
        return conceptView;
    }

    @Test
    public void testPopulateConceptsFlatVocabulary() throws Exception {
        String vocabularyName = "flatVocab";
        ConceptView concept1 = createConceptView(1L, "Term1", null);
        ConceptView concept2 = createConceptView(2L, "Term2", null);

        PagingResponse<ConceptView> response = new PagingResponse<>();
        response.setResults(List.of(concept1, concept2));
        response.setEndOfRecords(true);

        when(conceptClient.listConceptsLatestRelease(eq(vocabularyName), any(ConceptListParams.class)))
            .thenReturn(response);

        vocabularyConceptService.populateConceptsForVocabulary(vocabularyName);

        // Verify concepts were created
        verify(grScicollVocabConceptMapper, times(2)).create(conceptDtoCaptor.capture());

        List<GrSciCollVocabConceptDto> capturedDtos = conceptDtoCaptor.getAllValues();
        assertEquals(2, capturedDtos.size());

        // Verify concept details
        GrSciCollVocabConceptDto dto1 = capturedDtos.stream().filter(d -> d.getName().equals("Term1")).findFirst().orElse(null);
        assertNotNull(dto1);
        assertEquals("term1", dto1.getPath());
        assertEquals(vocabularyName, dto1.getVocabularyName());

        GrSciCollVocabConceptDto dto2 = capturedDtos.stream().filter(d -> d.getName().equals("Term2")).findFirst().orElse(null);
        assertNotNull(dto2);
        assertEquals("term2", dto2.getPath());
        assertEquals(vocabularyName, dto2.getVocabularyName());
    }

    @Test
    public void testPopulateConceptsNestedVocabulary() throws Exception {
        String vocabularyName = "nestedVocab";
        ConceptView parent = createConceptView(1L, "Parent", null);
        ConceptView child = createConceptView(2L, "Child", 1L);

        PagingResponse<ConceptView> response = new PagingResponse<>();
        response.setResults(List.of(parent, child));
        response.setEndOfRecords(true);

        when(conceptClient.listConceptsLatestRelease(eq(vocabularyName), any(ConceptListParams.class)))
            .thenReturn(response);

        vocabularyConceptService.populateConceptsForVocabulary(vocabularyName);

        // Verify concepts were created
        verify(grScicollVocabConceptMapper, times(2)).create(conceptDtoCaptor.capture());

        List<GrSciCollVocabConceptDto> capturedDtos = conceptDtoCaptor.getAllValues();
        assertEquals(2, capturedDtos.size());

        // Verify hierarchical path
        GrSciCollVocabConceptDto childDto = capturedDtos.stream()
            .filter(d -> d.getName().equals("Child"))
            .findFirst().orElse(null);
        assertNotNull(childDto);
        assertEquals("parent.child", childDto.getPath());
    }

    @Test
    public void testPopulateConceptsNameSanitization() throws Exception {
        String vocabularyName = "sanitizeVocab";
        ConceptView concept1 = createConceptView(1L, "Term With Spaces", null);
        ConceptView concept2 = createConceptView(2L, "Term-With-Hyphens_And_Underscores", null);
        ConceptView concept3 = createConceptView(3L, "TermWith!@#SpecialChars", null);
        ConceptView concept4 = createConceptView(4L, " Parent with Space ", null);
        ConceptView childOfSpace = createConceptView(5L, "Child Of Space", 4L);

        PagingResponse<ConceptView> response = new PagingResponse<>();
        response.setResults(List.of(concept1, concept2, concept3, concept4, childOfSpace));
        response.setEndOfRecords(true);

        when(conceptClient.listConceptsLatestRelease(eq(vocabularyName), any(ConceptListParams.class)))
            .thenReturn(response);

        vocabularyConceptService.populateConceptsForVocabulary(vocabularyName);

        verify(grScicollVocabConceptMapper, times(5)).create(conceptDtoCaptor.capture());

        List<GrSciCollVocabConceptDto> capturedDtos = conceptDtoCaptor.getAllValues();
        Map<String, String> expectedPaths = Map.of(
            "Term With Spaces", "term_with_spaces",
            "Term-With-Hyphens_And_Underscores", "term-with-hyphens_and_underscores",
            "TermWith!@#SpecialChars", "termwithspecialchars", // !@# removed
            " Parent with Space ", "parent_with_space", // leading/trailing spaces trimmed, then sanitized
            "Child Of Space", "parent_with_space.child_of_space"
        );

        for (GrSciCollVocabConceptDto dto : capturedDtos) {
            assertEquals(vocabularyName, dto.getVocabularyName());
            assertNotNull(expectedPaths.get(dto.getName()), "Unexpected concept name in sanitization test: " + dto.getName());
            assertEquals(expectedPaths.get(dto.getName()), dto.getPath(), "Path mismatch for sanitized name " + dto.getName());
        }
    }

    @Test
    public void testPopulateConceptsPaging() throws Exception {
        String vocabularyName = "pagedVocab";

        ConceptView concept1 = createConceptView(1L, "Page1Term1", null);
        ConceptView concept2 = createConceptView(2L, "Page1Term2", null);
        ConceptView concept3 = createConceptView(3L, "Page2Term1", null);

        PagingResponse<ConceptView> page1Response = new PagingResponse<>(0, 2, 3L);
        page1Response.setResults(List.of(concept1, concept2));
        page1Response.setEndOfRecords(false);

        PagingResponse<ConceptView> page2Response = new PagingResponse<>(2, 1, 3L);
        page2Response.setResults(List.of(concept3));
        page2Response.setEndOfRecords(true);

        // Mock conceptClient to return page1 then page2
        when(conceptClient.listConceptsLatestRelease(eq(vocabularyName), any(ConceptListParams.class)))
            .thenReturn(page1Response)
            .thenReturn(page2Response);

        vocabularyConceptService.populateConceptsForVocabulary(vocabularyName);

        // Expect 3 concepts to be processed in total
        verify(grScicollVocabConceptMapper, times(3)).create(conceptDtoCaptor.capture());

        List<GrSciCollVocabConceptDto> capturedDtos = conceptDtoCaptor.getAllValues();
        assertEquals(3, capturedDtos.size());
        assertNotNull(capturedDtos.stream().filter(d -> d.getName().equals("Page1Term1") && d.getPath().equals("page1term1")).findFirst().orElse(null));
        assertNotNull(capturedDtos.stream().filter(d -> d.getName().equals("Page1Term2") && d.getPath().equals("page1term2")).findFirst().orElse(null));
        assertNotNull(capturedDtos.stream().filter(d -> d.getName().equals("Page2Term1") && d.getPath().equals("page2term1")).findFirst().orElse(null));

        // Verify conceptClient was called twice
        verify(conceptClient, times(2)).listConceptsLatestRelease(eq(vocabularyName), any(ConceptListParams.class));
    }

    @Test
    public void testPopulateConceptsOrphanParent() throws Exception {
        String vocabularyName = "orphanVocab";
        // Parent with key 1L is deliberately missing from the response
        ConceptView childWithOrphanParent = createConceptView(2L, "OrphanChild", 1L); // ParentKey 1L does not exist
        ConceptView standaloneConcept = createConceptView(3L, "Standalone", null);

        PagingResponse<ConceptView> response = new PagingResponse<>();
        // Only provide the child and another standalone concept
        response.setResults(List.of(childWithOrphanParent, standaloneConcept));
        response.setEndOfRecords(true);

        when(conceptClient.listConceptsLatestRelease(eq(vocabularyName), any(ConceptListParams.class)))
            .thenReturn(response);

        vocabularyConceptService.populateConceptsForVocabulary(vocabularyName);

        verify(grScicollVocabConceptMapper, times(2)).create(conceptDtoCaptor.capture());

        List<GrSciCollVocabConceptDto> capturedDtos = conceptDtoCaptor.getAllValues();
        assertEquals(2, capturedDtos.size());

        GrSciCollVocabConceptDto orphanDto = capturedDtos.stream().filter(d -> d.getName().equals("OrphanChild")).findFirst().orElse(null);
        assertNotNull(orphanDto);
        // The service logs a warning and prepends _orphanparent_
        assertNotNull(orphanDto.getPath());
        assertEquals(vocabularyName, orphanDto.getVocabularyName());

        GrSciCollVocabConceptDto standaloneDto = capturedDtos.stream().filter(d -> d.getName().equals("Standalone")).findFirst().orElse(null);
        assertNotNull(standaloneDto);
        assertEquals("standalone", standaloneDto.getPath());
        assertEquals(vocabularyName, standaloneDto.getVocabularyName());
    }

    @Test
    public void testPopulateConceptsMaxDepth() throws Exception {
        String vocabularyName = "deepVocab";
        final int MAX_DEPTH = 20; // As defined in VocabularyConceptsService
        List<ConceptView> concepts = new ArrayList<>();
        Long parentKey = null;

        for (int i = 0; i < MAX_DEPTH + 5; i++) {
            Long currentKey = (long) i + 1;
            String name = "Level" + i;
            ConceptView concept = createConceptView(currentKey, name, parentKey);
            concepts.add(concept);
            parentKey = currentKey;
        }

        PagingResponse<ConceptView> response = new PagingResponse<>();
        response.setResults(concepts);
        response.setEndOfRecords(true);

        when(conceptClient.listConceptsLatestRelease(eq(vocabularyName), any(ConceptListParams.class)))
            .thenReturn(response);

        vocabularyConceptService.populateConceptsForVocabulary(vocabularyName);

        // All concepts should still be processed and created
        verify(grScicollVocabConceptMapper, times(MAX_DEPTH + 5)).create(conceptDtoCaptor.capture());

        List<GrSciCollVocabConceptDto> capturedDtos = conceptDtoCaptor.getAllValues();
        GrSciCollVocabConceptDto deepDto = capturedDtos.stream()
            .filter(d -> d.getName().equals("Level" + (MAX_DEPTH + 4)))
            .findFirst().orElse(null);
        assertNotNull(deepDto);

        String[] pathSegments = deepDto.getPath().split("\\.");
        assertEquals(MAX_DEPTH, pathSegments.length, "Path should be truncated to MAX_DEPTH segments.");
        assertEquals("level" + (MAX_DEPTH + 4), pathSegments[MAX_DEPTH - 1]); // Last segment is the node itself
        // The first segment of the truncated path for "Level24" (index i = 24) should be "level5" (index i = 5)
        // Because Level24 -> Level23 ... -> Level5 (this is the 20th element going up)
        assertEquals("level" + ((MAX_DEPTH + 4) - (MAX_DEPTH - 1)), pathSegments[0]);
    }

}
