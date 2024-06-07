package org.gbif.registry.test.mocks;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.AddTagAction;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.api.DeprecateConceptAction;
import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.vocabulary.model.*;
import org.gbif.vocabulary.model.search.KeyNameResult;

import java.util.*;

public class ConceptClientMock implements ConceptClient {

  public static final String ROOT_CONCEPT = "test1";
  public static final String CHILD1 = "test1.1";
  public static final String CHILD11 = "test1.1.1";
  public static final String CHILD2 = "test1.2";

  private static final Map<String, ConceptView> CONCEPTS = new HashMap<>();

  static {
    Concept concept1 = new Concept();
    concept1.setName(ROOT_CONCEPT);
    ConceptView conceptView1 = new ConceptView(concept1);
    conceptView1.setChildren(Arrays.asList(CHILD1, CHILD2));

    Concept concept11 = new Concept();
    concept11.setName(CHILD1);
    ConceptView conceptView11 = new ConceptView(concept11);
    conceptView11.setChildren(Collections.singletonList(CHILD11));

    Concept concept12 = new Concept();
    concept12.setName(CHILD2);
    ConceptView conceptView12 = new ConceptView(concept12);

    CONCEPTS.put(concept1.getName(), conceptView1);
    CONCEPTS.put(concept11.getName(), conceptView11);
    CONCEPTS.put(concept12.getName(), conceptView12);
  }

  @Override
  public PagingResponse<ConceptView> listConcepts(String s, ConceptListParams conceptListParams) {
    return new PagingResponse<>();
  }

  @Override
  public ConceptView get(String s, String s1, boolean b, boolean b1) {
    return null;
  }

  @Override
  public ConceptView create(String s, Concept concept) {
    return null;
  }

  @Override
  public ConceptView update(String s, String s1, Concept concept) {
    return null;
  }

  @Override
  public List<KeyNameResult> suggest(String s, SuggestParams suggestParams) {
    return null;
  }

  @Override
  public void deprecate(String s, String s1, DeprecateConceptAction deprecateConceptAction) {}

  @Override
  public void restoreDeprecated(String s, String s1, boolean b) {}

  @Override
  public List<Definition> listDefinitions(String s, String s1, ListParams listParams) {
    return null;
  }

  @Override
  public Definition getDefinition(String s, String s1, long l) {
    return null;
  }

  @Override
  public Definition addDefinition(String s, String s1, Definition definition) {
    return null;
  }

  @Override
  public Definition updateDefinition(String s, String s1, long l, Definition definition) {
    return null;
  }

  @Override
  public void deleteDefinition(String s, String s1, long l) {}

  @Override
  public List<Tag> listTags(String s, String s1) {
    return null;
  }

  @Override
  public void addTag(String s, String s1, AddTagAction addTagAction) {}

  @Override
  public void removeTag(String s, String s1, String s2) {}

  @Override
  public List<Label> listLabels(String s, String s1, ListParams listParams) {
    return null;
  }

  @Override
  public PagingResponse<Label> listAlternativeLabels(String s, String s1, ListParams listParams) {
    return null;
  }

  @Override
  public PagingResponse<HiddenLabel> listHiddenLabels(
      String s, String s1, PagingRequest pagingRequest) {
    return null;
  }

  @Override
  public Long addLabel(String s, String s1, Label label) {
    return null;
  }

  @Override
  public Long addAlternativeLabel(String s, String s1, Label label) {
    return null;
  }

  @Override
  public Long addHiddenLabel(String s, String s1, HiddenLabel hiddenLabel) {
    return null;
  }

  @Override
  public void deleteLabel(String s, String s1, long l) {}

  @Override
  public void deleteAlternativeLabel(String s, String s1, long l) {}

  @Override
  public void deleteHiddenLabel(String s, String s1, long l) {}

  @Override
  public PagingResponse<ConceptView> listConceptsLatestRelease(
      String s, ConceptListParams conceptListParams) {

    ConceptView view = CONCEPTS.get(conceptListParams.getName());
    if (view == null) {
      return new PagingResponse<>(conceptListParams.getPage(), 0L, Collections.emptyList());
    } else {
      return new PagingResponse<>(conceptListParams.getPage(), 1L, Collections.singletonList(view));
    }
  }

  @Override
  public ConceptView getFromLatestRelease(
      String vocabName, String conceptName, boolean b, boolean b1) {
    if (vocabName == null || conceptName == null || conceptName.equals("foo")) {
      return null;
    }

    Concept concept = new Concept();
    concept.setName(conceptName);
    return new ConceptView(concept);
  }

  @Override
  public List<Definition> listDefinitionsFromLatestRelease(
      String s, String s1, ListParams listParams) {
    return null;
  }

  @Override
  public List<Label> listLabelsFromLatestRelease(String s, String s1, ListParams listParams) {
    return null;
  }

  @Override
  public PagingResponse<Label> listAlternativeLabelsFromLatestRelease(
      String s, String s1, ListParams listParams) {
    return null;
  }

  @Override
  public PagingResponse<HiddenLabel> listHiddenLabelsFromLatestRelease(
      String s, String s1, ListParams listParams) {
    return null;
  }

  @Override
  public List<KeyNameResult> suggestLatestRelease(String s, SuggestParams suggestParams) {
    return null;
  }
}
