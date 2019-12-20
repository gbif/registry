package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.StepType;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PipelineExecutionTableEntryTransformer
  implements TableEntryTransformer<PipelineExecution> {

  @Override
  public PipelineExecution transform(Map<String, String> entry) {
    PipelineExecution result = new PipelineExecution();
    Optional.ofNullable(entry.get("stepsToRun"))
      .map(p -> p.split(","))
      .map(p ->
        Arrays.stream(p)
          .map(StepType::valueOf)
          .collect(Collectors.toList()))
      .ifPresent(result::setStepsToRun);

    result.setRerunReason(entry.get("rerunReason"));
    result.setRemarks(entry.get("remarks"));

    return result;
  }
}
