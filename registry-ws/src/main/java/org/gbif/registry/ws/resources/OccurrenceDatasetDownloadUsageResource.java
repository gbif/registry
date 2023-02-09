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
package org.gbif.registry.ws.resources;

import org.gbif.api.model.occurrence.DownloadType;
import org.gbif.registry.persistence.mapper.DatasetOccurrenceDownloadMapper;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

/** Occurrence download resource/web service. */
/*
 * OpenAPI documentation:
 *
 * This class has OpenAPI/SpringDoc method annotations, but the tag is the same
 * as in occurrence→occurrence-ws→OccurrenceDownloadResource.
 *
 * The result is manually moved from the Registry OpenAPI document to the
 * Occurrence OpenAPI document.
 */
@Tag(name = "Occurrence downloads")
@Validated
@RestController("datasetOccurrenceDownloadUsageResource")
@RequestMapping(value = "occurrence/download/dataset", produces = MediaType.APPLICATION_JSON_VALUE)
public class OccurrenceDatasetDownloadUsageResource extends DatasetDownloadUsageResourceBase {

  public OccurrenceDatasetDownloadUsageResource(
      DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper) {
    super(datasetOccurrenceDownloadMapper, DownloadType.OCCURRENCE);
  }
}
