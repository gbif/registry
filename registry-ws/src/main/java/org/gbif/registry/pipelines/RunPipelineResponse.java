package org.gbif.registry.pipelines;

import org.gbif.api.model.crawler.pipelines.StepType;

import java.util.Objects;
import java.util.Set;

/**
 * Encapsulates the possible response of the request of re-execute a pipeline of dataset and and attempt.
 */
public class RunPipelineResponse {

  /**
   * Possible response statuses.
   */
  public enum ResponseStatus {
    OK,
    PIPELINE_IN_SUBMITTED,
    UNSUPPORTED_STEP,
    ERROR
  }


  private final ResponseStatus responseStatus;

  private final Set<StepType> steps;

  public RunPipelineResponse(ResponseStatus responseStatus, Set<StepType> steps) {
    this.responseStatus = responseStatus;
    this.steps = steps;
  }

  /**
   * @return the response status of execution request
   */
  public ResponseStatus getResponseStatus() {
    return responseStatus;
  }

  /**
   * @return steps requested to be executed
   */
  public Set<StepType> getSteps() {
    return steps;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RunPipelineResponse that = (RunPipelineResponse) o;
    return responseStatus == that.responseStatus && steps.equals(that.steps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(responseStatus, steps);
  }

  /**
   * @return a new Builder instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * @return a new Builder instance.
   */
  public static Builder builder(RunPipelineResponse runPipelineResponse) {
    Builder builder = new Builder();
    return builder.setStep(runPipelineResponse.steps)
                  .setResponseStatus(runPipelineResponse.responseStatus);
  }

  /**
   * Builder for {@link RunPipelineResponse}.
   */
  public static class Builder {

    private ResponseStatus responseStatus;
    private Set<StepType> step;

    public Builder setResponseStatus(ResponseStatus responseStatus) {
      this.responseStatus = responseStatus;
      return this;
    }

    public Builder setStep(Set<StepType> step) {
      this.step = step;
      return this;
    }

    public RunPipelineResponse build() {
      return new RunPipelineResponse(responseStatus, step);
    }
  }
}
