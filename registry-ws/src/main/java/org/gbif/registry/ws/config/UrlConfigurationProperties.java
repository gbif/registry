/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.ws.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "url")
public class UrlConfigurationProperties {

  private String api;

  private String portal;

  private String occurrenceApi;

  private String directoryApi;

  private String grscicollPortal;

  public String getApi() {
    return api;
  }

  public void setApi(String api) {
    this.api = api;
  }

  public String getPortal() {
    return portal;
  }

  public void setPortal(String portal) {
    this.portal = portal;
  }

  public String getOccurrenceApi() {
    return occurrenceApi;
  }

  public void setOccurrenceApi(String occurrenceApi) {
    this.occurrenceApi = occurrenceApi;
  }

  public String getDirectoryApi() {
    return directoryApi;
  }

  public void setDirectoryApi(String directoryApi) {
    this.directoryApi = directoryApi;
  }

  public String getGrscicollPortal() {
    return grscicollPortal;
  }

  public void setGrscicollPortal(String grscicollPortal) {
    this.grscicollPortal = grscicollPortal;
  }
}
