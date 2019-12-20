package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.ws.PipelineStepParameters;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
          .collect(Collectors.toList())
      );
    }

    Optional.ofNullable(entry.get("status"))
      .map(PipelineStep.Status::valueOf)
      .ifPresent(result::setStatus);

    return result;
  }
}
