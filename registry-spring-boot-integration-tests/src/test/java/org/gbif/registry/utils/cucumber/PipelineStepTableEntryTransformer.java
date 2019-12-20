package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.StepRunner;
import org.gbif.api.model.pipelines.StepType;

import java.util.Map;
import java.util.Optional;

public class PipelineStepTableEntryTransformer
  implements TableEntryTransformer<PipelineStep> {

  @Override
  public PipelineStep transform(Map<String, String> entry) {
    PipelineStep result = new PipelineStep();
    result.setMessage(entry.get("message"));
    Optional.ofNullable(entry.get("runner"))
      .map(StepRunner::valueOf)
      .ifPresent(result::setRunner);
    Optional.ofNullable(entry.get("type"))
      .map(StepType::valueOf)
      .ifPresent(result::setType);
    Optional.ofNullable(entry.get("state"))
      .map(PipelineStep.Status::valueOf)
      .ifPresent(result::setState);

    return result;
  }
}
