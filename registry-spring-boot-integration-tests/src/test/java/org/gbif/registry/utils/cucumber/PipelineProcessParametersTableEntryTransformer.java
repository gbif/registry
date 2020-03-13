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

import org.gbif.api.model.pipelines.ws.PipelineProcessParameters;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.cucumber.datatable.TableEntryTransformer;

public class PipelineProcessParametersTableEntryTransformer
    implements TableEntryTransformer<PipelineProcessParameters> {

  @Override
  public PipelineProcessParameters transform(Map<String, String> entry) {
    PipelineProcessParameters parameters = new PipelineProcessParameters();
    Optional.ofNullable(entry.get("attempt"))
        .map(Integer::parseInt)
        .ifPresent(parameters::setAttempt);
    Optional.ofNullable(entry.get("datasetKey"))
        .map(UUID::fromString)
        .ifPresent(parameters::setDatasetKey);

    return parameters;
  }
}
