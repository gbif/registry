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
package org.gbif.registry.search.dataset.indexing.ws.taxon;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.JsonNode;

public interface TaxonApiClient {

  @RequestMapping(
    value = "dataset/{datasetKey}/metrics",
    method = RequestMethod.GET,
    produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  JsonNode getMetrics(@PathVariable("datasetKey") UUID datasetKey);

  @RequestMapping(
    value = "taxon/search/{datasetKey}",
    method = RequestMethod.GET,
    produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  JsonNode search(@PathVariable("datasetKey") String datasetKey, @RequestParam(required = false) MultiValueMap<String, String> queryParams);

}
