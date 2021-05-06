package org.gbif.registry.domain.mail;

import java.net.URL;

/**
 * Model to generate an email to notify the creation of a GRSciColl change suggestion.
 *
 * <p>This class is required to be public for Freemarker.
 */
public class GrscicollChangeSuggestionDataModel {

  private URL changeSuggestionUrl;

  public URL getChangeSuggestionUrl() {
    return changeSuggestionUrl;
  }

  public void setChangeSuggestionUrl(URL changeSuggestionUrl) {
    this.changeSuggestionUrl = changeSuggestionUrl;
  }
}
