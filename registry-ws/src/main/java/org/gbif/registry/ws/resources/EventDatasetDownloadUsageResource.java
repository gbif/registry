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

/** Event download resource/web service. */
@Validated
@RestController
@RequestMapping(value = "event/download/dataset", produces = MediaType.APPLICATION_JSON_VALUE)
public class EventDatasetDownloadUsageResource
    extends DatasetDownloadUsageResourceBase {

  public EventDatasetDownloadUsageResource(DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper) {
    super(datasetOccurrenceDownloadMapper, DownloadType.EVENT);
  }

}
