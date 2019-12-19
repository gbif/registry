package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.model.pipelines.ws.PipelineProcessParameters;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
