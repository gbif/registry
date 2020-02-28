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

/** Base template used to generate an email of the type "Hello [name], please click [url]. */
public class BaseTemplateDataModel {

  private final String name;
  private final URL url;

  public BaseTemplateDataModel(String name, URL url) {
    this.name = name;
    this.url = url;
  }

  public String getName() {
    return name;
  }

  public URL getUrl() {
    return url;
  }
}
