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

import java.util.List;

public class ConceptClientMock implements ConceptClient {
  @Override
  public PagingResponse<ConceptView> listConcepts(String s, ConceptListParams conceptListParams) {
    return null;
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
    return null;
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
