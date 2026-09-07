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
package org.gbif.registry.search.dataset.service;

import org.gbif.api.model.registry.ApproximateCounts;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Reads approximate occurrence / name-usage counts from the dataset search index.
 * Counts are index-time snapshots and may be stale relative to live metrics.
 */
@Slf4j
@Service
public class DatasetApproximateCountsService {

  private final ElasticsearchClient elasticsearchClient;
  private final String index;

  @Autowired
  public DatasetApproximateCountsService(
      @Value("${elasticsearch.registry.index}") String index,
      ElasticsearchClient elasticsearchClient) {
    this.index = index;
    this.elasticsearchClient = elasticsearchClient;
  }

  /**
   * @return approximate counts if the dataset document exists in ES; empty on miss or error
   */
  public Optional<ApproximateCounts> get(UUID datasetKey) {
    if (datasetKey == null) {
      return Optional.empty();
    }
    try {
      GetResponse<ObjectNode> response =
          elasticsearchClient.get(g -> g.index(index).id(datasetKey.toString()), ObjectNode.class);
      if (!response.found() || response.source() == null) {
        return Optional.empty();
      }
      ObjectNode source = response.source();
      ApproximateCounts counts = new ApproximateCounts();
      if (source.hasNonNull("occurrenceCount")) {
        counts.setOccurrenceCount(source.get("occurrenceCount").asLong());
      }
      if (source.hasNonNull("nameUsagesCount")) {
        counts.setNameUsageCount(source.get("nameUsagesCount").asLong());
      }
      if (counts.getOccurrenceCount() == null && counts.getNameUsageCount() == null) {
        return Optional.empty();
      }
      return Optional.of(counts);
    } catch (Exception e) {
      log.warn("Failed to load approximate counts for dataset {}", datasetKey, e);
      return Optional.empty();
    }
  }
}
