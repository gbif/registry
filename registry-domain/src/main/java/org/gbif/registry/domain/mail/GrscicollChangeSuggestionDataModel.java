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
