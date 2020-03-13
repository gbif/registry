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
package org.gbif.registry.utils.cucumber;

import org.gbif.api.model.collections.Institution;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import io.cucumber.datatable.TableEntryTransformer;

public class InstitutionTableEntryTransformer implements TableEntryTransformer<Institution> {

  @Override
  public Institution transform(Map<String, String> entry) {
    Institution result = new Institution();

    result.setCode(entry.get("code"));
    result.setName(entry.get("name"));
    result.setDescription(entry.get("description"));
    Optional.ofNullable(entry.get("homepage")).map(URI::create).ifPresent(result::setHomepage);

    return result;
  }
}
