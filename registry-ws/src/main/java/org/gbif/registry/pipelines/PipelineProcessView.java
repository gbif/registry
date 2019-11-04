package org.gbif.registry.pipelines;

import org.gbif.api.model.pipelines.PipelineProcess;

import org.codehaus.jackson.annotate.JsonUnwrapped;

/** View with the information to show in the registry UI. */
public class PipelineProcessView {

  @JsonUnwrapped private PipelineProcess process;
  private String datasetTitle;

  public PipelineProcess getProcess() {
    return process;
  }

  public void setProcess(PipelineProcess process) {
    this.process = process;
  }

  public String getDatasetTitle() {
    return datasetTitle;
  }

  public void setDatasetTitle(String datasetTitle) {
    this.datasetTitle = datasetTitle;
  }
}
