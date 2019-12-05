package org.gbif.registry.pipelines.model;

import org.gbif.api.model.pipelines.StepType;

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

  private final String message;

  public RunPipelineResponse(ResponseStatus responseStatus, Set<StepType> steps, String message) {
    this.responseStatus = responseStatus;
    this.steps = steps;
    this.message = message;
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

  public String getMessage() {
    return message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RunPipelineResponse that = (RunPipelineResponse) o;
    return responseStatus == that.responseStatus && steps.equals(that.steps) && message.equals(that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(responseStatus, steps, message);
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
    private String message;

    public Builder setResponseStatus(ResponseStatus responseStatus) {
      this.responseStatus = responseStatus;
      return this;
    }

    public Builder setStep(Set<StepType> step) {
      this.step = step;
      return this;
    }

    public Builder setMessage(String message) {
      this.message = message;
      return this;
    }

    public RunPipelineResponse build() {
      return new RunPipelineResponse(responseStatus, step, message);
    }
  }
}
