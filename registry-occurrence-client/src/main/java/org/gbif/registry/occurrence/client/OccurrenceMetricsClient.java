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
package org.gbif.registry.occurrence.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/** Access to Occurrence metrics. */
@FeignClient(value = "OccurrenceMetricsClient")
@RequestMapping("occurrence")
public interface OccurrenceMetricsClient {

  /**
   * Get the number of record for a datasetKey.
   *
   * @param datasetKey dataset identifier (UUID)
   * @return number of occurrences
   */
  @SuppressWarnings("squid:S4488")
  @RequestMapping(
      method = RequestMethod.GET,
      value = "count",
      produces = MediaType.APPLICATION_JSON_VALUE)
  Long getCountForDataset(@RequestParam("datasetKey") UUID datasetKey);
}
