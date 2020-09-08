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
package org.gbif.registry.service;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.registry.domain.ws.CitationDatasetUsage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

public interface RegistryDatasetService {

  Dataset get(UUID key);

  PagingResponse<Dataset> augmentWithMetadata(PagingResponse<Dataset> resp);

  @Nullable
  Dataset getPreferredMetadataDataset(UUID key);

  List<Metadata> listMetadata(UUID datasetKey, @Nullable MetadataType type);

  byte[] getMetadataDocument(int metadataKey);

  List<CitationDatasetUsage> ensureCitationDatasetUsagesValid(Map<String, Long> data);
}
