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
package org.gbif.registry.oaipmh;

import javax.annotation.Nullable;

/** Wrapper for OAI-PMH request parameters. */
public class OaipmhRequestParameters {

  private String verb;
  private String identifier;
  private String metadataPrefix;
  private String from;
  private String until;
  private String set;
  private String resumptionToken;

  public String getVerb() {
    return verb;
  }

  public void setVerb(String verb) {
    this.verb = verb;
  }

  @Nullable
  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(@Nullable String identifier) {
    this.identifier = identifier;
  }

  @Nullable
  public String getMetadataPrefix() {
    return metadataPrefix;
  }

  public void setMetadataPrefix(@Nullable String metadataPrefix) {
    this.metadataPrefix = metadataPrefix;
  }

  @Nullable
  public String getFrom() {
    return from;
  }

  public void setFrom(@Nullable String from) {
    this.from = from;
  }

  @Nullable
  public String getUntil() {
    return until;
  }

  public void setUntil(@Nullable String until) {
    this.until = until;
  }

  @Nullable
  public String getSet() {
    return set;
  }

  public void setSet(@Nullable String set) {
    this.set = set;
  }

  @Nullable
  public String getResumptionToken() {
    return resumptionToken;
  }

  public void setResumptionToken(@Nullable String resumptionToken) {
    this.resumptionToken = resumptionToken;
  }
}
