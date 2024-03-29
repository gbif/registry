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
package org.gbif.registry.domain.ws;

import org.gbif.api.util.HttpURI;

import java.io.Serializable;
import java.net.URI;
import java.util.Objects;
import java.util.StringJoiner;

public class DerivedDatasetUpdateRequest implements Serializable {

  private URI sourceUrl;
  private String title;
  private String description;

  @HttpURI
  public URI getSourceUrl() {
    return sourceUrl;
  }

  public void setSourceUrl(URI sourceUrl) {
    this.sourceUrl = sourceUrl;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DerivedDatasetUpdateRequest that = (DerivedDatasetUpdateRequest) o;
    return Objects.equals(sourceUrl, that.sourceUrl)
        && Objects.equals(title, that.title)
        && Objects.equals(description, that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceUrl, title, description);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", DerivedDatasetUpdateRequest.class.getSimpleName() + "[", "]")
        .add("sourceUrl=" + sourceUrl)
        .add("title=" + title)
        .add("description=" + description)
        .toString();
  }
}
