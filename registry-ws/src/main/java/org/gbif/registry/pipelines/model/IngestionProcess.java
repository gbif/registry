package org.gbif.registry.pipelines.model;

import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.model.pipelines.PipelineExecution;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class IngestionProcess {

  private UUID datasetKey;
  private String datasetTitle;
  private int attempt;
  private DatasetProcessStatus crawlInfo;
  private Set<PipelineExecution> pipelineExecutions;

  public UUID getDatasetKey() {
    return datasetKey;
  }

  public IngestionProcess setDatasetKey(UUID datasetKey) {
    this.datasetKey = datasetKey;
    return this;
  }

  public String getDatasetTitle() {
    return datasetTitle;
  }

  public IngestionProcess setDatasetTitle(String datasetTitle) {
    this.datasetTitle = datasetTitle;
    return this;
  }

  public int getAttempt() {
    return attempt;
  }

  public IngestionProcess setAttempt(int attempt) {
    this.attempt = attempt;
    return this;
  }

  public DatasetProcessStatus getCrawlInfo() {
    return crawlInfo;
  }

  public IngestionProcess setCrawlInfo(DatasetProcessStatus crawlInfo) {
    this.crawlInfo = crawlInfo;
    return this;
  }

  public Set<PipelineExecution> getPipelineExecutions() {
    return pipelineExecutions;
  }

  public IngestionProcess setPipelineExecutions(Set<PipelineExecution> pipelineExecutions) {
    this.pipelineExecutions = pipelineExecutions;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IngestionProcess that = (IngestionProcess) o;
    return attempt == that.attempt && Objects.equals(datasetKey, that.datasetKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(datasetKey, attempt);
  }
}
