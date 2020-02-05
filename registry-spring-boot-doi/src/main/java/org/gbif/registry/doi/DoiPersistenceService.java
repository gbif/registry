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
package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.api.model.common.paging.Pageable;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/** DOI data access object. */
public interface DoiPersistenceService {

  DoiData get(DOI doi);

  DoiType getType(DOI doi);

  String getMetadata(DOI doi);

  List<Map<String, Object>> list(
      @Nullable DoiStatus status, @Nullable DoiType type, @Nullable Pageable page);

  void create(DOI doi, DoiType type);

  void update(DOI doi, DoiData status, String xml);

  void delete(DOI doi);
}
