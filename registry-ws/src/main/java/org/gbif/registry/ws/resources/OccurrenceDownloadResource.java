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
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.registry.doi.DoiIssuingService;
import org.gbif.registry.doi.DownloadDoiDataCiteHandlingService;
import org.gbif.registry.persistence.mapper.DatasetOccurrenceDownloadMapper;
import org.gbif.registry.persistence.mapper.OccurrenceDownloadMapper;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Occurrence download resource/web service. */
@Validated
@RestController("occurrenceDownloadResource")
@RequestMapping(value = "occurrence/download", produces = MediaType.APPLICATION_JSON_VALUE)
public class OccurrenceDownloadResource extends BaseDownloadResource {

  public OccurrenceDownloadResource(
      OccurrenceDownloadMapper occurrenceDownloadMapper,
      DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper,
      DoiIssuingService doiIssuingService,
      @Lazy DownloadDoiDataCiteHandlingService doiDataCiteHandlingService,
      @Qualifier("baseIdentityAccessService") IdentityAccessService identityService) {
    super(
        occurrenceDownloadMapper,
        datasetOccurrenceDownloadMapper,
        doiIssuingService,
        doiDataCiteHandlingService,
        identityService,
        DownloadType.OCCURRENCE);
  }
}
