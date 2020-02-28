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
import org.gbif.api.model.pipelines.StepRunner;
import org.gbif.api.model.pipelines.StepType;

import java.util.Map;
import java.util.Optional;

import io.cucumber.datatable.TableEntryTransformer;

public class PipelineStepTableEntryTransformer implements TableEntryTransformer<PipelineStep> {

  @Override
  public PipelineStep transform(Map<String, String> entry) {
    PipelineStep result = new PipelineStep();
    result.setMessage(entry.get("message"));
    Optional.ofNullable(entry.get("runner")).map(StepRunner::valueOf).ifPresent(result::setRunner);
    Optional.ofNullable(entry.get("type")).map(StepType::valueOf).ifPresent(result::setType);
    Optional.ofNullable(entry.get("state"))
        .map(PipelineStep.Status::valueOf)
        .ifPresent(result::setState);

    return result;
  }
}
