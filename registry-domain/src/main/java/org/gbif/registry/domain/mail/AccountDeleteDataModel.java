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
package org.gbif.registry.domain.mail;

import java.net.URL;
import java.util.Collections;
import java.util.List;

public class AccountDeleteDataModel extends BaseTemplateDataModel {

  private final List<String> downloadUrls;

  public AccountDeleteDataModel(String name, URL url, List<String> downloadUrls) {
    super(name, url);
    this.downloadUrls = downloadUrls != null ? downloadUrls : Collections.emptyList();
  }

  public List<String> getDownloadUrls() {
    return Collections.unmodifiableList(downloadUrls);
  }
}
