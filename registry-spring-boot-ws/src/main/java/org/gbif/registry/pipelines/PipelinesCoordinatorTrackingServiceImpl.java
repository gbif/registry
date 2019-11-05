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
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.PipelineWorkflow;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.model.pipelines.WorkflowStep;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.util.comparators.EndpointCreatedComparator;
import org.gbif.api.util.comparators.EndpointPriorityComparator;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.PipelinesAbcdMessage;
import org.gbif.common.messaging.api.messages.PipelinesDwcaMessage;
import org.gbif.common.messaging.api.messages.PipelinesInterpretedMessage;
import org.gbif.common.messaging.api.messages.PipelinesVerbatimMessage;
import org.gbif.common.messaging.api.messages.PipelinesXmlMessage;
import org.gbif.registry.persistence.mapper.pipelines.PipelineProcessMapper;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Service that allows to re-run pipeline steps on an specific attempt.
 */
@Service
public class PipelinesCoordinatorTrackingServiceImpl implements PipelinesHistoryTrackingService {

  private static final Logger LOG =
    LoggerFactory.getLogger(PipelinesCoordinatorTrackingServiceImpl.class);

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

  private static final Comparator<Endpoint> ENDPOINT_COMPARATOR = Ordering.compound(Lists.newArrayList(
    Collections.reverseOrder(new EndpointPriorityComparator()),
    EndpointCreatedComparator.INSTANCE
  ));

  /**
   * The messagePublisher can be optional.
   */
  private final MessagePublisher publisher;

  // MyBatis mapper
  private final PipelineProcessMapper mapper;
  private final DatasetService datasetService;
  private final MetricsHandler metricsHandler;

  public PipelinesCoordinatorTrackingServiceImpl(
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
  private void doOnAllDatasets(Consumer<Dataset> onDataset) {
    PagingRequest pagingRequest = new PagingRequest(0, PAGE_SIZE);

    PagingResponse<Dataset> response;
    do {
      response = datasetService.list(pagingRequest);
      response
        .getResults()
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
      getEndpointToCrawl(dataset).ifPresent(endpoint -> {
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
    } else if (steps.contains(StepType.INTERPRETED_TO_INDEX) || steps.contains(StepType.HDFS_VIEW)) {
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
  public RunPipelineResponse runLastAttempt(Set<StepType> steps, String reason, String user) {
    String prefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    CompletableFuture.runAsync(
      () -> doOnAllDatasets(dataset -> runLastAttempt(dataset.getKey(), steps, reason, user, prefix))
    );

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
    return pipelineProcess.getSteps().stream()
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

    // Collects the latest steps per type.
    Set<PipelineStep.Status> statuses = new HashSet<>();
    for (StepType stepType : StepType.values()) {
      pipelineProcess.getSteps().stream()
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
    statuses.forEach(
      p -> {
        addNumberRecords(p);
        setDatasetTitle(p);
      });

    return new PagingResponse<>(pageable, count, statuses);
  }

  @Override
  public PagingResponse<PipelineProcess> history(UUID datasetKey, Pageable pageable) {
    Objects.requireNonNull(datasetKey, "DatasetKey can't be null");

    long count = mapper.count(datasetKey, null);
    List<PipelineProcess> statuses = mapper.list(datasetKey, null, pageable);

    // add needed fields for the view
    statuses.forEach(
      p -> {
        addNumberRecords(p);
        setDatasetTitle(p);
      });

    return new PagingResponse<>(pageable, count, statuses);
  }

  @Override
  public RunPipelineResponse runPipelineAttempt(
    UUID datasetKey, int attempt, Set<StepType> steps, String reason, String user, String prefix) {
    Objects.requireNonNull(datasetKey, "DatasetKey can't be null");
    Objects.requireNonNull(steps, "Steps can't be null");
    Objects.requireNonNull(reason, "Reason can't be null");
    Objects.requireNonNull(publisher, "No message publisher configured");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(user), "user can't be null");

    PipelineProcess status = mapper.getByDatasetAndAttempt(datasetKey, attempt);

    // Checks that the pipelines is not in RUNNING state
    if (getStatus(status) == PipelineStep.Status.RUNNING) {
      return new RunPipelineResponse.Builder()
        .setResponseStatus(RunPipelineResponse.ResponseStatus.PIPELINE_IN_SUBMITTED)
        .setStep(steps)
        .build();
    }

    // Performs the messaging and updates the status onces the message has been sent
    RunPipelineResponse.Builder responseBuilder = RunPipelineResponse.builder().setStep(steps);
    Dataset dataset = datasetService.get(datasetKey);
    prioritizeSteps(steps, dataset).forEach(
      stepName ->
        getLatestSuccessfulStep(status, stepName)
          .ifPresent(
            step -> {
              try {
                Message message = null;

                if (stepName == StepType.INTERPRETED_TO_INDEX) {
                  PipelinesInterpretedMessage m = OBJECT_MAPPER.readValue(step.getMessage(), PipelinesInterpretedMessage.class);
                  Optional.ofNullable(prefix).ifPresent(m::setResetPrefix);
                  m.setOnlyForStep(StepType.INTERPRETED_TO_INDEX.name());
                  m.setPipelineSteps(Collections.singleton(StepType.INTERPRETED_TO_INDEX.name()));
                  message = m;
                } else if (stepName == StepType.HDFS_VIEW) {
                  PipelinesInterpretedMessage m = OBJECT_MAPPER.readValue(step.getMessage(), PipelinesInterpretedMessage.class);
                  Optional.ofNullable(prefix).ifPresent(m::setResetPrefix);
                  m.setOnlyForStep(StepType.HDFS_VIEW.name());
                  m.setPipelineSteps(Collections.singleton(StepType.HDFS_VIEW.name()));
                  message = m;
                } else if (stepName == StepType.VERBATIM_TO_INTERPRETED) {
                  PipelinesVerbatimMessage m = OBJECT_MAPPER.readValue(step.getMessage(), PipelinesVerbatimMessage.class);
                  Optional.ofNullable(prefix).ifPresent(m::setResetPrefix);
                  m.setPipelineSteps(new HashSet<>(Arrays.asList(StepType.VERBATIM_TO_INTERPRETED.name(), StepType.INTERPRETED_TO_INDEX.name(), StepType.HDFS_VIEW.name())));
                  message = m;
                } else if (stepName == StepType.DWCA_TO_VERBATIM) {
                  message = OBJECT_MAPPER.readValue(step.getMessage(), PipelinesDwcaMessage.class);
                } else if (stepName == StepType.ABCD_TO_VERBATIM) {
                  message = OBJECT_MAPPER.readValue(step.getMessage(), PipelinesAbcdMessage.class);
                } else if (stepName == StepType.XML_TO_VERBATIM) {
                  message = OBJECT_MAPPER.readValue(step.getMessage(), PipelinesXmlMessage.class);
                } else {
                  responseBuilder.setResponseStatus(RunPipelineResponse.ResponseStatus.UNSUPPORTED_STEP);
                }

                if (message != null) {
                  responseBuilder.setResponseStatus(RunPipelineResponse.ResponseStatus.OK);
                  publisher.send(message);
                }

                // update rerun reason and modifier user
                mapper.updatePipelineStep(step.setRerunReason(reason).setModifiedBy(user));
              } catch (IOException ex) {
                LOG.error("Error reading message", ex);
                throw Throwables.propagate(ex);
              }
            }));

    return responseBuilder.build();
  }

  @Override
  public PipelineProcess get(UUID datasetKey, int attempt) {
    Objects.requireNonNull(datasetKey, "DatasetKey can't be null");

    PipelineProcess process = mapper.getByDatasetAndAttempt(datasetKey, attempt);

    // get number of records
    return addNumberRecords(process);
  }

  @Override
  public long create(UUID datasetKey, int attempt, String creator) {
    Objects.requireNonNull(datasetKey, "DatasetKey can't be null");
    Objects.requireNonNull(creator, "Creator can't be null");

    PipelineProcess pipelineProcess = new PipelineProcess();
    pipelineProcess.setDatasetKey(datasetKey);
    pipelineProcess.setAttempt(attempt);
    pipelineProcess.setCreatedBy(creator);
    mapper.create(pipelineProcess);

    return pipelineProcess.getKey();
  }

  @Override
  public long addPipelineStep(long pipelineProcessKey, PipelineStep pipelineStep, String user) {
    Objects.requireNonNull(pipelineStep, "PipelineStep can't be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(user), "user can't be null");

    pipelineStep.setCreatedBy(user);
    mapper.addPipelineStep(pipelineProcessKey, pipelineStep);
    return pipelineStep.getKey();
  }

  @Override
  public PipelineStep getPipelineStep(long key) {
    return mapper.getPipelineStep(key);
  }

  @Override
  public void updatePipelineStepStatusAndMetrics(
    long processKey, long pipelineStepKey, PipelineStep.Status status, String user) {
    Objects.requireNonNull(status, "Status can't be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(user), "user can't be null");

    // fetch entities
    PipelineStep step = mapper.getPipelineStep(pipelineStepKey);
    PipelineProcess process = mapper.get(processKey);
    Preconditions.checkArgument(process.getSteps().contains(step), "The process doesn't contain the step.");

    if (step.getState() != status
      && (PipelineStep.Status.FAILED == status || PipelineStep.Status.COMPLETED == status)) {
      step.setFinished(LocalDateTime.now());
    }

    // get metrics
    step.setMetrics(
      metricsHandler.getMetricInfo(
        process.getDatasetKey(),
        process.getAttempt(),
        step.getType(),
        step.getStarted(),
        step.getFinished()));

    // update status and modifying user
    step.setState(status);
    step.setModifiedBy(user);

    mapper.updatePipelineStep(step);
  }

  @Override
  public PipelineWorkflow getPipelineWorkflow(UUID datasetKey, int attempt) {
    Objects.requireNonNull(datasetKey, "datasetKey can't be null");

    PipelineProcess process = mapper.getByDatasetAndAttempt(datasetKey, attempt);

    // group the steps by its execution order in the workflow and then by name. This will create
    // something
    // like:
    // 1 -> DWCA_TO_AVRO -> List<PipelineStep>
    // 2 -> VERBATIM_TO_INTERPRETED -> List<PipelineStep>
    // 3 -> INTERPRETED_TO_INDEX -> List<PipelineStep>
    //   -> HIVE_VIEW -> List<PipelineStep>
    //
    // The map is sorted by the step execution order in descending order
    Map<Integer, Map<StepType, List<PipelineStep>>> stepsByOrderAndName =
      process.getSteps().stream()
        .collect(
          Collectors.groupingBy(
            s -> s.getType().getExecutionOrder(),
            () ->
              new TreeMap<Integer, Map<StepType, List<PipelineStep>>>(
                Comparator.reverseOrder()),
            Collectors.groupingBy(PipelineStep::getType)));

    // iterate from steps in the last position to the ones in the first one so that we can create
    // the worfklow hierarchy
    List<WorkflowStep> stepsPreviousIteration = null;
    for (Map.Entry<Integer, Map<StepType, List<PipelineStep>>> stepsByOrder :
      stepsByOrderAndName.entrySet()) {

      List<WorkflowStep> currentSteps = new ArrayList<>();
      for (Map.Entry<StepType, List<PipelineStep>> stepsByType :
        stepsByOrder.getValue().entrySet()) {

        // create workflow step
        WorkflowStep step = new WorkflowStep();
        step.setStepType(stepsByType.getKey());
        step.getAllSteps().addAll(stepsByType.getValue());
        step.setLastStep(step.getAllSteps().iterator().next());
        // link this step to its next steps
        step.setNextSteps(stepsPreviousIteration);

        // accumulate all the steps of this StepType
        currentSteps.add(step);
      }

      // the steps of this iteration now become the steps of the previous iteration
      stepsPreviousIteration = currentSteps;
    }

    // create workflow
    PipelineWorkflow workflow = new PipelineWorkflow();
    workflow.setDatasetKey(process.getDatasetKey());
    workflow.setAttempt(process.getAttempt());
    // the last steps created will be the started steps of the workflow
    workflow.setSteps(stepsPreviousIteration);

    return workflow;
  }


  //Copied from CrawlerCoordinatorServiceImpl

  /**
   * Gets the endpoint that we want to crawl from the passed in dataset.
   * <p/>
   * We take into account a list of supported and prioritized endpoint types and verify that the declared dataset type
   * matches a supported endpoint type.
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

  //Copied from CrawlerCoordinatorServiceImpl
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

  private PipelineProcess addNumberRecords(PipelineProcess process) {
    if (process != null && process.getSteps() != null) {
      process.getSteps().stream()
        .filter(s -> s.getType().getExecutionOrder() == 1)
        .max(Comparator.comparing(PipelineStep::getStarted))
        .ifPresent(
          s -> {
            try {
              if (s.getType() == StepType.DWCA_TO_VERBATIM) {
                PipelinesDwcaMessage message =
                  OBJECT_MAPPER.readValue(s.getMessage(), PipelinesDwcaMessage.class);
                if (message != null
                  && message.getValidationReport() != null
                  && message.getValidationReport().getOccurrenceReport() != null) {
                  process.setNumberRecords(
                    message.getValidationReport().getOccurrenceReport().getCheckedRecords());
                }
              } else if (s.getType() == StepType.XML_TO_VERBATIM) {
                PipelinesXmlMessage message =
                  OBJECT_MAPPER.readValue(s.getMessage(), PipelinesXmlMessage.class);
                if (message != null) {
                  process.setNumberRecords(message.getTotalRecordCount());
                }
              } // abcd doesn't have count
            } catch (IOException ex) {
              LOG.warn(
                "Couldn't get the number of records for dataset {} and attempt {}",
                process.getDatasetKey(),
                process.getAttempt(),
                ex);
            }
          });
    }

    return process;
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
