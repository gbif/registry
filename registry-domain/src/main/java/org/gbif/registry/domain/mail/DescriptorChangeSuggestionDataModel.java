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
package org.gbif.registry.domain.mail;

import org.gbif.api.model.collections.suggestions.Type;

import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Model to generate an email to notify the creation of a descriptor change suggestion.
 *
 * <p>This class is required to be public for Freemarker.
 */
public class DescriptorChangeSuggestionDataModel {

  private URL changeSuggestionUrl;
  private Type suggestionType;
  private String collectionName;
  private String collectionCountry;
  private String title;
  private String description;
  private String format;
  private List<String> comments;
  private Set<String> tags;
  private String proposerEmail;

  public URL getChangeSuggestionUrl() {
    return changeSuggestionUrl;
  }

  public void setChangeSuggestionUrl(URL changeSuggestionUrl) {
    this.changeSuggestionUrl = changeSuggestionUrl;
  }

  public Type getSuggestionType() {
    return suggestionType;
  }

  public void setSuggestionType(Type suggestionType) {
    this.suggestionType = suggestionType;
  }

  public String getCollectionName() {
    return collectionName;
  }

  public void setCollectionName(String collectionName) {
    this.collectionName = collectionName;
  }

  public String getCollectionCountry() {
    return collectionCountry;
  }

  public void setCollectionCountry(String collectionCountry) {
    this.collectionCountry = collectionCountry;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public List<String> getComments() {
    return comments;
  }

  public void setComments(List<String> comments) {
    this.comments = comments;
  }

  public Set<String> getTags() {
    return tags;
  }

  public void setTags(Set<String> tags) {
    this.tags = tags;
  }

  public String getProposerEmail() {
    return proposerEmail;
  }

  public void setProposerEmail(String proposerEmail) {
    this.proposerEmail = proposerEmail;
  }
} 