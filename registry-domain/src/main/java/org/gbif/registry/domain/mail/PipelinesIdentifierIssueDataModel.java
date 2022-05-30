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

/**
 * Model to generate an email to notify about the issue with identifiers during interpretation.
 *
 * <p>This class is required to be public for Freemarker.
 */
public class PipelinesIdentifierIssueDataModel {

  private String registryUrl;

  private String datasetKey;

  private int attempt;

  private String datasetName;

  private String message;

  public PipelinesIdentifierIssueDataModel(
      String registryUrl, String datasetKey, int attempt, String datasetName, String message) {
    this.registryUrl = registryUrl;
    this.datasetKey = datasetKey;
    this.attempt = attempt;
    this.datasetName = datasetName;
    this.message = message;
  }

  public static PipelinesIdentifierIssueDataModel build(
      String registryUrl, String datasetKey, int attempt, String datasetName, String message) {
    return new PipelinesIdentifierIssueDataModel(
        registryUrl, datasetKey, attempt, datasetName, message);
  }

  public String getRegistryUrl() {
    return registryUrl;
  }

  public String getDatasetKey() {
    return datasetKey;
  }

  public int getAttempt() {
    return attempt;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public String getMessage() {
    return message;
  }
}
