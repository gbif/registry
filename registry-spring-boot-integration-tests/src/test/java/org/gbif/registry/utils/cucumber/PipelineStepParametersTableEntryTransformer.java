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

import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.ws.PipelineStepParameters;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.cucumber.datatable.TableEntryTransformer;

public class PipelineStepParametersTableEntryTransformer
    implements TableEntryTransformer<PipelineStepParameters> {

  @Override
  public PipelineStepParameters transform(Map<String, String> entry) {
    PipelineStepParameters result = new PipelineStepParameters();

    String metrics = entry.get("metrics");
    if (metrics != null) {
      result.setMetrics(
          Arrays.stream(entry.get("metrics").split(","))
              .map(p -> p.split("=>"))
              .map(p -> new PipelineStep.MetricInfo(p[0], p[1]))
              .collect(Collectors.toList()));
    }

    Optional.ofNullable(entry.get("status"))
        .map(PipelineStep.Status::valueOf)
        .ifPresent(result::setStatus);

    return result;
  }
}
