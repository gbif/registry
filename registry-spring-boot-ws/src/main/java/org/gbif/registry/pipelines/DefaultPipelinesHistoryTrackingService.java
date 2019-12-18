package org.gbif.registry.pipelines;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.util.comparators.EndpointCreatedComparator;
import org.gbif.api.util.comparators.EndpointPriorityComparator;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.PipelineBasedMessage;
import org.gbif.common.messaging.api.messages.PipelinesAbcdMessage;
import org.gbif.common.messaging.api.messages.PipelinesDwcaMessage;
import org.gbif.common.messaging.api.messages.PipelinesInterpretedMessage;
import org.gbif.common.messaging.api.messages.PipelinesVerbatimMessage;
import org.gbif.common.messaging.api.messages.PipelinesXmlMessage;
import org.gbif.registry.persistence.mapper.pipelines.PipelineProcessMapper;
import org.gbif.registry.pipelines.model.RunPipelineResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service that allows to re-run pipeline steps on an specific attempt.
 */
@Service
public class DefaultPipelinesHistoryTrackingService implements PipelinesHistoryTrackingService {

  private static final Logger LOG =
    LoggerFactory.getLogger(DefaultPipelinesHistoryTrackingService.class);

  // Used to iterate over all datasets
  private static final int PAGE_SIZE = 200;

  // Used to read serialized messages stored in the data base as strings.
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    // determines whether encountering of unknown properties (ones that do not map to a property,
    // and there is no
    // "any setter" or handler that can handle it) should result in a failure (throwing a
    // JsonMappingException) or not.
    OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // Enforce use of ISO-8601 format dates (http://wiki.fasterxml.com/JacksonFAQDateHandling)
    OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  private static final Comparator<Endpoint> ENDPOINT_COMPARATOR =
    Ordering.compound(
      Lists.newArrayList(
        Collections.reverseOrder(new EndpointPriorityComparator()),
        EndpointCreatedComparator.INSTANCE));

  /**
   * The messagePublisher can be optional.
   */
  private final MessagePublisher publisher;

  // MyBatis mapper
  private final PipelineProcessMapper mapper;
  private final DatasetService datasetService;
  private final MetricsHandler metricsHandler;

  public DefaultPipelinesHistoryTrackingService(
    @Autowired(required = false) MessagePublisher publisher,
    PipelineProcessMapper mapper,
    @Lazy DatasetService datasetService,
    MetricsHandler metricsHandler) {
    this.publisher = publisher;
    this.mapper = mapper;
    this.datasetService = datasetService;
    this.metricsHandler = metricsHandler;
  }

  @Override
  public RunPipelineResponse runLastAttempt(
    UUID datasetKey, Set<StepType> steps, String reason, String user, String prefix) {
    int lastAttempt =
      mapper
        .getLastAttempt(datasetKey)
        .orElseThrow(
          () ->
            new IllegalArgumentException(
              "Couldn't find last attempt for dataset " + datasetKey));
    return runPipelineAttempt(datasetKey, lastAttempt, steps, reason, user, prefix);
  }

  /**
   * Utility method to run batch jobs on all dataset elements
   */
  private void doOnAllDatasets(Consumer<Dataset> onDataset, List<UUID> datasetsToExclude) {
    PagingRequest pagingRequest = new PagingRequest(0, PAGE_SIZE);

    PagingResponse<Dataset> response;
    do {
      response = datasetService.list(pagingRequest);
      response.getResults().stream()
        .filter(d -> datasetsToExclude == null || !datasetsToExclude.contains(d.getKey()))
        .forEach(
          d -> {
            try {
              LOG.info("trying to rerun dataset {}", d.getKey());
              onDataset.accept(d);
            } catch (Exception ex) {
              LOG.error(
                "Error processing dataset {} while rerunning all datasets: {}",
                d.getKey(),
                ex.getMessage());
            }
          });
      pagingRequest.addOffset(response.getResults().size());
    } while (!response.isEndOfRecords());
  }

  private Set<StepType> prioritizeSteps(Set<StepType> steps, Dataset dataset) {
    Set<StepType> newSteps = new HashSet<>();
    if (steps.contains(StepType.TO_VERBATIM)) {
      getEndpointToCrawl(dataset)
        .ifPresent(
          endpoint -> {
            if (EndpointType.DWC_ARCHIVE == endpoint.getType()) {
              newSteps.add(StepType.DWCA_TO_VERBATIM);
            } else if (EndpointType.BIOCASE_XML_ARCHIVE == endpoint.getType()) {
              newSteps.add(StepType.ABCD_TO_VERBATIM);
            } else {
              newSteps.add(StepType.XML_TO_VERBATIM);
            }
          });
    } else if (steps.contains(StepType.ABCD_TO_VERBATIM)) {
      newSteps.add(StepType.ABCD_TO_VERBATIM);
    } else if (steps.contains(StepType.XML_TO_VERBATIM)) {
      newSteps.add(StepType.XML_TO_VERBATIM);
    } else if (steps.contains(StepType.DWCA_TO_VERBATIM)) {
      newSteps.add(StepType.DWCA_TO_VERBATIM);
    } else if (steps.contains(StepType.VERBATIM_TO_INTERPRETED)) {
      newSteps.add(StepType.VERBATIM_TO_INTERPRETED);
    } else if (steps.contains(StepType.INTERPRETED_TO_INDEX)
      || steps.contains(StepType.HDFS_VIEW)) {
      if (steps.contains(StepType.INTERPRETED_TO_INDEX)) {
        newSteps.add(StepType.INTERPRETED_TO_INDEX);
      }
      if (steps.contains(StepType.HDFS_VIEW)) {
        newSteps.add(StepType.HDFS_VIEW);
      }
    }
    return newSteps;
  }

  @Override
  public RunPipelineResponse runLastAttempt(
    Set<StepType> steps, String reason, String user, List<UUID> datasetsToExclude) {
    String prefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    CompletableFuture.runAsync(
      () ->
        doOnAllDatasets(
          dataset -> runLastAttempt(dataset.getKey(), steps, reason, user, prefix),
          datasetsToExclude));

    return RunPipelineResponse.builder()
      .setResponseStatus(RunPipelineResponse.ResponseStatus.OK)
      .setStep(steps)
      .build();
  }

  /**
   * Search the last step executed of a specific StepType.
   *
   * @param pipelineProcess container of steps
   * @param step            to be searched
   * @return optionally, the las step found
   */
  private Optional<PipelineStep> getLatestSuccessfulStep(
    PipelineProcess pipelineProcess, StepType step) {
    return pipelineProcess.getExecutions().stream()
      .sorted(Comparator.comparing(PipelineExecution::getCreated).reversed())
      .flatMap(ex -> ex.getSteps().stream())
      .filter(s -> step.equals(s.getType()))
      .max(Comparator.comparing(PipelineStep::getStarted));
  }

  /**
   * Calculates the general state of a {@link PipelineProcess}. If one the latest steps of a
   * specific {@link StepType} has a {@link PipelineStep.Status#FAILED}, the process is considered
   * as FAILED. If all the latest steps of all {@link StepType} have the same {@link
   * PipelineStep.Status}, that status used for the {@link PipelineProcess}. If it has step in
   * {@link PipelineStep.Status#RUNNING} it is decided as the process status, otherwise is {@link
   * PipelineStep.Status#COMPLETED}
   *
   * @param pipelineProcess that contains all the steps.
   * @return the calculated status of a {@link PipelineProcess}
   */
  private PipelineStep.Status getStatus(PipelineProcess pipelineProcess) {
    // get last execution
    PipelineExecution lastExecution =
      pipelineProcess.getExecutions().stream()
        .max(Comparator.comparing(PipelineExecution::getCreated))
        .orElseThrow(
          () ->
            new IllegalStateException(
              "Couldn't fina las execution for process: " + pipelineProcess));

    // Collects the latest steps per type.
    Set<PipelineStep.Status> statuses = new HashSet<>();
    for (StepType stepType : StepType.values()) {
      lastExecution.getSteps().stream()
        .filter(s -> stepType == s.getType())
        .max(Comparator.comparing(PipelineStep::getStarted))
        .ifPresent(step -> statuses.add(step.getState()));
    }

    // Only has one states, it could means that all steps have the same status
    if (statuses.size() == 1) {
      return statuses.iterator().next();
    } else {
      // Checks the states by priority
      if (statuses.contains(PipelineStep.Status.FAILED)) {
        return PipelineStep.Status.FAILED;
      } else if (statuses.contains(PipelineStep.Status.RUNNING)) {
        return PipelineStep.Status.RUNNING;
      } else {
        return PipelineStep.Status.COMPLETED;
      }
    }
  }

  @Override
  public PagingResponse<PipelineProcess> history(Pageable pageable) {
    long count = mapper.count(null, null);
    List<PipelineProcess> statuses = mapper.list(null, null, pageable);

    // add needed fields for the view
    statuses.forEach(this::setDatasetTitle);

    return new PagingResponse<>(pageable, count, statuses);
  }

  @Override
  public PagingResponse<PipelineProcess> history(UUID datasetKey, Pageable pageable) {
    Objects.requireNonNull(datasetKey, "DatasetKey can't be null");

    long count = mapper.count(datasetKey, null);
    List<PipelineProcess> statuses = mapper.list(datasetKey, null, pageable);

    // add needed fields for the view
    statuses.forEach(this::setDatasetTitle);

    return new PagingResponse<>(pageable, count, statuses);
  }

  @Override
  public RunPipelineResponse runPipelineAttempt(
    UUID datasetKey,
    int attempt,
    Set<StepType> steps,
    String reason,
    String user,
    String prefix) {
    Objects.requireNonNull(datasetKey, "DatasetKey can't be null");
    Objects.requireNonNull(steps, "Steps can't be null");
    Objects.requireNonNull(reason, "Reason can't be null");
    Objects.requireNonNull(publisher, "No message publisher configured");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(user), "user can't be null");

    PipelineProcess process = mapper.getByDatasetAndAttempt(datasetKey, attempt);

    // Checks that the pipelines is not in RUNNING state
    if (getStatus(process) == PipelineStep.Status.RUNNING) {
      return new RunPipelineResponse.Builder()
        .setResponseStatus(RunPipelineResponse.ResponseStatus.PIPELINE_IN_SUBMITTED)
        .setStep(steps)
        .build();
    }

    // Performs the messaging and updates the status onces the message has been sent
    RunPipelineResponse.Builder responseBuilder = RunPipelineResponse.builder().setStep(steps);
    Dataset dataset = datasetService.get(datasetKey);
    prioritizeSteps(steps, dataset)
      .forEach(
        stepName ->
          getLatestSuccessfulStep(process, stepName)
            .ifPresent(
              step -> {
                try {
                  PipelineBasedMessage message = null;

                  if (stepName == StepType.INTERPRETED_TO_INDEX
                    || stepName == StepType.HDFS_VIEW) {
                    message =
                      createInterpretedMessage(prefix, step.getMessage(), stepName);
                  } else if (stepName == StepType.VERBATIM_TO_INTERPRETED) {
                    message = createVerbatimMessage(prefix, step.getMessage());
                  } else if (stepName == StepType.DWCA_TO_VERBATIM) {
                    message =
                      createMessage(step.getMessage(), PipelinesDwcaMessage.class);
                  } else if (stepName == StepType.ABCD_TO_VERBATIM) {
                    message =
                      createMessage(step.getMessage(), PipelinesAbcdMessage.class);
                  } else if (stepName == StepType.XML_TO_VERBATIM) {
                    message = createMessage(step.getMessage(), PipelinesXmlMessage.class);
                  } else {
                    responseBuilder.setResponseStatus(
                      RunPipelineResponse.ResponseStatus.UNSUPPORTED_STEP);
                  }

                  if (message != null) {
                    // create pipelines execution
                    PipelineExecution execution =
                      new PipelineExecution()
                        .setCreatedBy(user)
                        .setRerunReason(reason)
                        .setStepsToRun(new ArrayList<>(steps));
                    mapper.addPipelineExecution(process.getKey(), execution);

                    message.setExecutionId(execution.getKey());
                    publisher.send(message);

                    responseBuilder.setResponseStatus(
                      RunPipelineResponse.ResponseStatus.OK);
                    publisher.send(message);
                  }

                } catch (IOException ex) {
                  LOG.error("Error reading message", ex);
                  throw Throwables.propagate(ex);
                }
              }));

    return responseBuilder.build();
  }

  private <T extends PipelineBasedMessage> T createMessage(String jsonMessage, Class<T> targetClass)
    throws IOException {
    return OBJECT_MAPPER.readValue(jsonMessage, targetClass);
  }

  private PipelineBasedMessage createVerbatimMessage(String prefix, String jsonMessage) throws IOException {
    PipelinesVerbatimMessage message =
      OBJECT_MAPPER.readValue(jsonMessage, PipelinesVerbatimMessage.class);
    Optional.ofNullable(prefix).ifPresent(message::setResetPrefix);
    message.setPipelineSteps(
      new HashSet<>(
        Arrays.asList(
          StepType.VERBATIM_TO_INTERPRETED.name(),
          StepType.INTERPRETED_TO_INDEX.name(),
          StepType.HDFS_VIEW.name())));

    return message;
  }

  private PipelineBasedMessage createInterpretedMessage(String prefix, String jsonMessage, StepType stepType)
    throws IOException {
    PipelinesInterpretedMessage message =
      OBJECT_MAPPER.readValue(jsonMessage, PipelinesInterpretedMessage.class);
    Optional.ofNullable(prefix).ifPresent(message::setResetPrefix);
    message.setOnlyForStep(stepType.name());
    message.setPipelineSteps(Collections.singleton(stepType.name()));
    return message;
  }

  @Override
  public PipelineProcess get(UUID datasetKey, int attempt) {
    Objects.requireNonNull(datasetKey, "DatasetKey can't be null");

    PipelineProcess process = mapper.getByDatasetAndAttempt(datasetKey, attempt);

    setDatasetTitle(process);

    return process;
  }

  @Override
  public long createOrGet(UUID datasetKey, int attempt, String creator) {
    Objects.requireNonNull(datasetKey, "DatasetKey can't be null");
    Objects.requireNonNull(creator, "Creator can't be null");

    PipelineProcess pipelineProcess = new PipelineProcess();
    pipelineProcess.setDatasetKey(datasetKey);
    pipelineProcess.setAttempt(attempt);
    pipelineProcess.setCreatedBy(creator);
    mapper.createIfNotExists(pipelineProcess);

    return pipelineProcess.getKey();
  }

  @Override
  public long addPipelineExecution(
    long pipelineProcessKey, PipelineExecution pipelineExecution, String creator) {
    Objects.requireNonNull(pipelineExecution, "pipelineExecution can't be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(creator), "creator can't be null");

    pipelineExecution.setCreatedBy(creator);

    mapper.addPipelineExecution(pipelineProcessKey, pipelineExecution);

    return pipelineExecution.getKey();
  }

  @Override
  public long addPipelineStep(
    long pipelineProcessKey, long executionKey, PipelineStep pipelineStep, String user) {
    Objects.requireNonNull(pipelineStep, "PipelineStep can't be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(user), "user can't be null");

    // TODO: check that execution belongs to the process??

    pipelineStep.setStarted(LocalDateTime.now());
    pipelineStep.setCreatedBy(user);

    mapper.addPipelineStep(executionKey, pipelineStep);
    return pipelineStep.getKey();
  }

  @Override
  public PipelineStep getPipelineStep(long key) {
    return mapper.getPipelineStep(key);
  }

  @Override
  public void updatePipelineStepStatusAndMetrics(
    long processKey,
    long executionKey,
    long pipelineStepKey,
    PipelineStep.Status status,
    Set<PipelineStep.MetricInfo> metrics,
    String user) {
    Objects.requireNonNull(status, "Status can't be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(user), "user can't be null");

    // fetch entities
    PipelineProcess process = mapper.get(processKey);
    PipelineExecution execution = mapper.getPipelineExecution(executionKey);
    PipelineStep step = mapper.getPipelineStep(pipelineStepKey);
    Preconditions.checkArgument(
      process.getExecutions().contains(execution), "The process doesn't contain the execution.");
    Preconditions.checkArgument(
      execution.getSteps().contains(step), "The execution doesn't contain the step.");

    if (step.getState() != status
      && (PipelineStep.Status.FAILED == status || PipelineStep.Status.COMPLETED == status)) {
      step.setFinished(LocalDateTime.now());
    }

    step.setMetrics(metrics);
    step.setNumberRecords(
      metrics.stream()
        .map(PipelineStep.MetricInfo::getValue)
        .map(Long::parseLong)
        .max(Comparator.naturalOrder())
        .orElse(null));

    // update status and modifying user
    step.setState(status);
    step.setModifiedBy(user);

    mapper.updatePipelineStep(step);
  }

  // Copied from CrawlerCoordinatorServiceImpl

  /**
   * Gets the endpoint that we want to crawl from the passed in dataset.
   *
   * <p>We take into account a list of supported and prioritized endpoint types and verify that the
   * declared dataset type matches a supported endpoint type.
   *
   * @param dataset to get the endpoint for
   * @return will be present if we found an eligible endpoint
   * @see EndpointPriorityComparator
   */
  private Optional<Endpoint> getEndpointToCrawl(Dataset dataset) {
    // Are any of the endpoints eligible to be crawled
    List<Endpoint> sortedEndpoints = prioritySortEndpoints(dataset.getEndpoints());
    if (sortedEndpoints.isEmpty()) {
      return Optional.empty();
    }
    Endpoint ep = sortedEndpoints.get(0);
    return Optional.ofNullable(ep);
  }

  // Copied from CrawlerCoordinatorServiceImpl
  private List<Endpoint> prioritySortEndpoints(List<Endpoint> endpoints) {

    // Filter out all Endpoints that we can't crawl
    List<Endpoint> result = Lists.newArrayList();
    for (Endpoint endpoint : endpoints) {
      if (EndpointPriorityComparator.PRIORITIES.contains(endpoint.getType())) {
        result.add(endpoint);
      }
    }

    // Sort the remaining ones
    result.sort(ENDPOINT_COMPARATOR);
    return result;
  }

  private void setDatasetTitle(PipelineProcess process) {
    if (process != null && process.getDatasetKey() != null) {
      Dataset dataset = datasetService.get(process.getDatasetKey());
      if (dataset != null) {
        process.setDatasetTitle(dataset.getTitle());
      }
    }
  }
}
