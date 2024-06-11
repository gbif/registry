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
package org.gbif.registry.service.collections.converters;

import java.util.Collections;
import java.util.List;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.AddTagAction;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.api.DeprecateConceptAction;
import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Definition;
import org.gbif.vocabulary.model.HiddenLabel;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.model.search.KeyNameResult;

public class ConceptClientMock implements ConceptClient {
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

    List<ConceptView> results = Collections.singletonList(new ConceptView(new Concept()));

    return new PagingResponse<>(conceptListParams.getPage(), Long.valueOf(results.size()), results);
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
