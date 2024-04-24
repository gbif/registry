/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.pipelines;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import java.util.stream.Collectors;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.pipelines.*;
import org.gbif.api.model.pipelines.InterpretationType.RecordType;
import org.gbif.api.model.pipelines.PipelineStep.Status;
import org.gbif.api.model.pipelines.ws.SearchResult;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.util.comparators.EndpointCreatedComparator;
import org.gbif.api.util.comparators.EndpointPriorityComparator;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.PipelineBasedMessage;
import org.gbif.common.messaging.api.messages.PipelinesAbcdMessage;
import org.gbif.common.messaging.api.messages.PipelinesBalancerMessage;
import org.gbif.common.messaging.api.messages.PipelinesDwcaMessage;
import org.gbif.common.messaging.api.messages.PipelinesEventsInterpretedMessage;
import org.gbif.common.messaging.api.messages.PipelinesEventsMessage;
import org.gbif.common.messaging.api.messages.PipelinesInterpretedMessage;
import org.gbif.common.messaging.api.messages.PipelinesVerbatimMessage;
import org.gbif.common.messaging.api.messages.PipelinesXmlMessage;
import org.gbif.registry.mail.BaseEmailModel;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.pipelines.PipelinesEmailManager;
import org.gbif.registry.persistence.mapper.pipelines.PipelineProcessMapper;
import org.gbif.registry.pipelines.issues.GithubApiClient;
import org.gbif.registry.pipelines.issues.GithubApiClient.Issue;
import org.gbif.registry.pipelines.issues.GithubApiClient.IssueComment;
import org.gbif.registry.pipelines.issues.GithubApiClient.IssueResult;
import org.gbif.registry.pipelines.issues.IssueCreator;
import org.gbif.registry.pipelines.util.PredicateUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import freemarker.template.TemplateException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/** Service that allows to re-run pipeline steps on a specific attempt. */
@Service
public class DefaultRegistryPipelinesHistoryTrackingService
    implements RegistryPipelinesHistoryTrackingService {

  private static final Logger LOG =
      LoggerFactory.getLogger(DefaultRegistryPipelinesHistoryTrackingService.class);

  private static final Comparator<Endpoint> ENDPOINT_COMPARATOR =
      Ordering.compound(
          Lists.newArrayList(
              Collections.reverseOrder(new EndpointPriorityComparator()),
              EndpointCreatedComparator.INSTANCE));

  private static final Set<StepType> INCLUDE_FRAGMENTER = new HashSet<>(Arrays.asList(
    StepType.DWCA_TO_VERBATIM,
    StepType.XML_TO_VERBATIM,
    StepType.ABCD_TO_VERBATIM,
    StepType.FRAGMENTER
  ));

  private ObjectMapper objectMapper;
  /** The messagePublisher can be optional. */
  private final MessagePublisher publisher;

  private final PipelineProcessMapper mapper;
  private final DatasetService datasetService;
  private final ExecutorService executorService;
  private final EmailSender emailSender;
  private final PipelinesEmailManager pipelinesEmailManager;
  private final GithubApiClient githubApiClient;
  private final IssueCreator issueCreator;
  private static final String DATASET_KEY_CANNOT_BE_NULL = "DatasetKey can't be null";

  public DefaultRegistryPipelinesHistoryTrackingService(
      @Qualifier("registryObjectMapper") ObjectMapper objectMapper,
      @Autowired(required = false) MessagePublisher publisher,
      PipelineProcessMapper mapper,
      @Lazy DatasetService datasetService,
      @Autowired EmailSender emailSender,
      @Autowired PipelinesEmailManager pipelinesEmailManager,
      @Autowired GithubApiClient githubApiClient,
      @Autowired IssueCreator issueCreator,
      @Value("${pipelines.doAllThreads}") Integer threadPoolSize) {
    this.objectMapper = objectMapper;
    this.publisher = publisher;
    this.mapper = mapper;
    this.datasetService = datasetService;
    this.emailSender = emailSender;
    this.pipelinesEmailManager = pipelinesEmailManager;
    this.githubApiClient = githubApiClient;
    this.issueCreator = issueCreator;
    this.executorService =
        Optional.ofNullable(threadPoolSize)
            .map(Executors::newFixedThreadPool)
            .orElse(Executors.newSingleThreadExecutor());
  }

  @Override
  public RunPipelineResponse runLastAttempt(
      UUID datasetKey,
      Set<StepType> steps,
      String reason,
      String user,
      String prefix,
      boolean useLastSuccessful,
      boolean markPreviousAttemptAsFailed,
      Set<String> interpretTypes) {
    int attempt = findAttempt(datasetKey, steps, useLastSuccessful);
    return runPipelineAttempt(
        datasetKey,
        attempt,
        steps,
        reason,
        user,
        prefix,
        markPreviousAttemptAsFailed,
        interpretTypes);
  }

  private int findAttempt(UUID datasetKey, Set<StepType> steps, boolean useLastSuccessful) {
    if (useLastSuccessful && steps.size() != 1) {
      throw new IllegalArgumentException(
          "When using the last successful attempt you must pass 1 and only 1 step");
    }

    Optional<Integer> attempt =
        useLastSuccessful
            ? mapper.getLastSuccessfulAttempt(datasetKey, steps.iterator().next())
            : mapper.getLastAttempt(datasetKey);

    return attempt.orElseThrow(
        () -> new IllegalArgumentException("Couldn't find last attempt for dataset " + datasetKey));
  }

  @Override
  public RunPipelineResponse runLastAttempt(
      Set<StepType> steps,
      String reason,
      String user,
      List<UUID> datasetsToExclude,
      List<UUID> datasetsToInclude,
      boolean useLastSuccessful,
      boolean markPreviousAttemptAsFailed,
      Set<String> interpretTypes) {
    String prefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    CompletableFuture.runAsync(
        () ->
            doOnAllDatasets(
                datasetKey ->
                    runLastAttempt(
                        datasetKey,
                        steps,
                        reason,
                        user,
                        prefix,
                        useLastSuccessful,
                        markPreviousAttemptAsFailed,
                        interpretTypes),
                datasetsToExclude,
                datasetsToInclude),
        executorService);

    return RunPipelineResponse.builder()
        .setResponseStatus(RunPipelineResponse.ResponseStatus.OK)
        .setSteps(steps)
        .build();
  }

  /** Utility method to run batch jobs on all dataset elements */
  private void doOnAllDatasets(
      Consumer<UUID> onDataset, List<UUID> datasetsToExclude, List<UUID> datasetsToInclude) {

    if (datasetsToInclude == null || datasetsToInclude.isEmpty()) {
      throw new RuntimeException("datasetsToExclude can't be null or empty");
    }

    datasetsToInclude.stream()
        .filter(PredicateUtils.not(datasetsToExclude::contains))
        .forEach(
            datasetKey ->
                CompletableFuture.runAsync(
                    () -> {
                      try {
                        LOG.info("trying to rerun dataset {}", datasetKey);
                        onDataset.accept(datasetKey);
                      } catch (Exception ex) {
                        LOG.error(
                            "Error processing dataset {} while rerunning all datasets: {}",
                            datasetKey,
                            ex.getMessage());
                      }
                    },
                    executorService));
  }

  private Set<StepType> prioritizeSteps(Set<StepType> steps, Dataset dataset) {
    Set<StepType> newSteps = new HashSet<>(steps);
    if (steps.contains(StepType.TO_VERBATIM)) {
      newSteps.remove(StepType.TO_VERBATIM);
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
    }

    return newSteps;
  }

  /**
   * Search the last step executed of a specific StepType.
   *
   * @param pipelineProcess container of steps
   * @param step to be searched
   * @return optionally, the las step found
   */
  @VisibleForTesting
  Optional<PipelineStep> getLatestSuccessfulStep(PipelineProcess pipelineProcess, StepType step) {
    return pipelineProcess.getExecutions().stream()
        .filter(ex-> !ex.getStepsToRun().isEmpty())
        .sorted(Comparator.comparing(PipelineExecution::getCreated).reversed())
        .flatMap(ex -> ex.getSteps().stream())
        .filter(s -> step.equals(s.getType()))
        .filter(s -> s.getMessage() != null && !s.getMessage().isEmpty())
        .max(Comparator.comparing(PipelineStep::getStarted));
  }

  private PipelineExecution getLastestExecution(PipelineProcess pipelineProcess) {
    return pipelineProcess.getExecutions().stream()
        .max(Comparator.comparing(PipelineExecution::getCreated))
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Couldn't find las execution for process: " + pipelineProcess));
  }

  /**
   * Calculates the general state of a {@link PipelineExecution}. If one the latest steps of a
   * specific {@link StepType} has a {@link Status#FAILED}, the process is considered as FAILED. If
   * all the latest steps of all {@link StepType} have the same {@link Status}, that status used for
   * the {@link PipelineExecution}. If it has step in {@link Status#RUNNING} it is decided as the
   * process status, otherwise is {@link Status#COMPLETED}
   *
   * @param execution that contains all the steps.
   * @return the calculated status of a {@link PipelineProcess}
   */
  private Status getStatus(PipelineExecution execution) {
    // Collects the latest steps per type.
    Set<Status> statuses = new HashSet<>();
    for (StepType stepType : StepType.values()) {
      execution.getSteps().stream()
          .filter(s -> stepType == s.getType())
          .max(Comparator.comparing(PipelineStep::getStarted))
          .ifPresent(step -> statuses.add(step.getState()));
    }

    // Only has one states, it could mean that all steps have the same status
    if (statuses.size() == 1) {
      return statuses.iterator().next();
    } else {
      // Checks the states by priority
      if (statuses.contains(Status.FAILED) || statuses.contains(Status.ABORTED)) {
        return Status.FAILED;
      } else if (statuses.contains(Status.RUNNING)
          || statuses.contains(Status.QUEUED)
          || statuses.contains(Status.SUBMITTED)) {
        return Status.RUNNING;
      } else {
        return Status.COMPLETED;
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
    Objects.requireNonNull(datasetKey, DATASET_KEY_CANNOT_BE_NULL);

    long count = mapper.count(datasetKey, null);
    List<PipelineProcess> statuses = mapper.list(datasetKey, null, pageable);

    // add needed fields for the view
    statuses.forEach(this::setDatasetTitle);

    return new PagingResponse<>(pageable, count, statuses);
  }

  @Transactional
  @Override
  public RunPipelineResponse runPipelineAttempt(
      UUID datasetKey,
      int attempt,
      Set<StepType> steps,
      String reason,
      String user,
      String prefix,
      boolean markPreviousAttemptAsFailed,
      Set<String> interpretTypes) {
    Objects.requireNonNull(datasetKey, DATASET_KEY_CANNOT_BE_NULL);
    Objects.requireNonNull(steps, "Steps can't be null");
    Objects.requireNonNull(reason, "Reason can't be null");
    Objects.requireNonNull(publisher, "No message publisher configured");
    Preconditions.checkArgument(StringUtils.isNotEmpty(user), "user can't be null");

    PipelineProcess process = mapper.getByDatasetAndAttempt(datasetKey, attempt);

    PipelineExecution lastestExecution = getLastestExecution(process);
    Status status = getStatus(lastestExecution);

    if (markPreviousAttemptAsFailed
        || (status == Status.FAILED && !lastestExecution.isFinished())) {
      markPipelineStatusAsAborted(lastestExecution.getKey());
    }

    // Checks that the pipelines is not in RUNNING state
    if (!markPreviousAttemptAsFailed && status == Status.RUNNING) {
      return new RunPipelineResponse.Builder()
          .setResponseStatus(RunPipelineResponse.ResponseStatus.PIPELINE_IN_SUBMITTED)
          .setSteps(steps)
          .build();
    }

    // Performs the messaging and updates the status onces the message has been sent
    Dataset dataset = datasetService.get(datasetKey);

    Map<StepType, PipelineBasedMessage> stepsToSend = new EnumMap<>(StepType.class);
    for (StepType stepName : prioritizeSteps(steps, dataset)) {
      processStep(stepName, process, prefix, interpretTypes, stepsToSend);
    }

    if (stepsToSend.isEmpty()) {
      return RunPipelineResponse.builder()
          .setSteps(steps)
          .setStepsFailed(steps)
          .setResponseStatus(RunPipelineResponse.ResponseStatus.ERROR)
          .setMessage("No steps found. Probably there is no steps of this type in the DB")
          .build();
    }

    // create pipelines execution
    PipelineExecution execution =
        new PipelineExecution()
            .setCreatedBy(user)
            .setRerunReason(reason)
            .setStepsToRun(getStepTypes(stepsToSend.keySet()));

    long executionKey = addPipelineExecution(process.getKey(), execution, user);

    // send messages
    Set<StepType> stepsFailed = new HashSet<>(stepsToSend.size());
    stepsToSend.forEach(
        (key, message) -> {
          message.setExecutionId(executionKey);
          try {
            if (message instanceof PipelinesInterpretedMessage
                || message instanceof PipelinesVerbatimMessage) {
              String nextMessageClassName = message.getClass().getSimpleName();
              String messagePayload = message.toString();
              publisher.send(new PipelinesBalancerMessage(nextMessageClassName, messagePayload));
            } else {
              publisher.send(message);
            }
          } catch (IOException ex) {
            LOG.warn("Error sending message", ex);
            stepsFailed.add(key);
          }
        });

    RunPipelineResponse.Builder responseBuilder =
        RunPipelineResponse.builder().setStepsFailed(stepsFailed);
    if (stepsFailed.size() == steps.size()) {
      responseBuilder
          .setResponseStatus(RunPipelineResponse.ResponseStatus.ERROR)
          .setMessage("All steps failed when publishing the messages");
    } else {
      responseBuilder.setResponseStatus(RunPipelineResponse.ResponseStatus.OK);
    }

    return responseBuilder.build();
  }

  private void processStep(StepType stepName, PipelineProcess process, String prefix,
      Set<String> interpretTypes, Map<StepType, PipelineBasedMessage> stepsToSend){
    Optional<PipelineStep> latestStepOpt = getLatestSuccessfulStep(process, stepName);

    if (!latestStepOpt.isPresent()) {
      LOG.warn("Can't find latest successful step for the datasetKey {}", process.getDatasetKey());
      return;
    }

    PipelineStep step = latestStepOpt.get();
    try {
      PipelineBasedMessage message = null;

      if (stepName == StepType.INTERPRETED_TO_INDEX
          || stepName == StepType.HDFS_VIEW
          || stepName == StepType.FRAGMENTER) {
        message = createInterpretedMessage(prefix, step.getMessage(), stepName);
      } else if (stepName == StepType.VERBATIM_TO_INTERPRETED) {
        message = createVerbatimMessage(prefix, step.getMessage(), interpretTypes);
      } else if (stepName == StepType.DWCA_TO_VERBATIM) {
        message = createMessage(step.getMessage(), PipelinesDwcaMessage.class);
      } else if (stepName == StepType.ABCD_TO_VERBATIM) {
        message = createMessage(step.getMessage(), PipelinesAbcdMessage.class);
      } else if (stepName == StepType.XML_TO_VERBATIM) {
        message = createMessage(step.getMessage(), PipelinesXmlMessage.class);
      } else if (stepName == StepType.EVENTS_VERBATIM_TO_INTERPRETED) {
        message = createMessage(step.getMessage(), PipelinesEventsMessage.class);
      } else if (stepName == StepType.EVENTS_INTERPRETED_TO_INDEX) {
        message = createMessage(step.getMessage(), PipelinesEventsInterpretedMessage.class);
      }

      if (message != null) {
        stepsToSend.put(stepName, message);
      }
    } catch (IOException ex) {
      LOG.warn("Error reading message", ex);
    }
  }

  @VisibleForTesting
  protected Set<StepType> getStepTypes(Set<StepType> stepsToSend) {
    Set<StepType> finalSteps =
        PipelinesWorkflow.getOccurrenceWorkflow().getAllNodesFor(stepsToSend);
    if (stepsToSend.stream().noneMatch(INCLUDE_FRAGMENTER::contains)) {
      finalSteps.remove(StepType.FRAGMENTER);
    }
    return finalSteps;
  }

  private <T extends PipelineBasedMessage> T createMessage(String jsonMessage, Class<T> targetClass)
      throws IOException {
    return objectMapper.readValue(jsonMessage, targetClass);
  }

  private PipelineBasedMessage createVerbatimMessage(
      String prefix, String jsonMessage, Set<String> interpretTypes) throws IOException {
    PipelinesVerbatimMessage message =
        objectMapper.readValue(jsonMessage, PipelinesVerbatimMessage.class);
    Optional.ofNullable(prefix).ifPresent(message::setResetPrefix);
    HashSet<String> steps = new HashSet<>();

    if (message.getPipelineSteps().contains(StepType.VERBATIM_TO_INTERPRETED.name())) {
      steps.add(StepType.VERBATIM_TO_INTERPRETED.name());
      steps.add(StepType.INTERPRETED_TO_INDEX.name());
      steps.add(StepType.HDFS_VIEW.name());
    }

    if (message.getPipelineSteps().contains(StepType.EVENTS_VERBATIM_TO_INTERPRETED.name())) {
      steps.add(StepType.EVENTS_VERBATIM_TO_INTERPRETED.name());
      steps.add(StepType.EVENTS_INTERPRETED_TO_INDEX.name());
      steps.add(StepType.EVENTS_HDFS_VIEW.name());
    }

    message.setPipelineSteps(steps);
    if (interpretTypes != null && !interpretTypes.isEmpty()) {
      message.setInterpretTypes(interpretTypes);
    } else {
      message.getInterpretTypes().remove(RecordType.IDENTIFIER_ABSENT.name());
      message.getInterpretTypes().add(RecordType.METADATA.name());
      message.getInterpretTypes().add(RecordType.BASIC.name());
      message.getInterpretTypes().add(RecordType.TEMPORAL.name());
      message.getInterpretTypes().add(RecordType.TAXONOMY.name());
      message.getInterpretTypes().add(RecordType.LOCATION.name());
      message.getInterpretTypes().add(RecordType.GRSCICOLL.name());
      message.getInterpretTypes().add(RecordType.CLUSTERING.name());
    }

    return message;
  }

  private PipelineBasedMessage createInterpretedMessage(
      String prefix, String jsonMessage, StepType stepType) throws IOException {
    PipelinesInterpretedMessage message =
        objectMapper.readValue(jsonMessage, PipelinesInterpretedMessage.class);
    Optional.ofNullable(prefix).ifPresent(message::setResetPrefix);
    message.setPipelineSteps(Collections.singleton(stepType.name()));
    return message;
  }

  @Override
  public PipelineProcess get(UUID datasetKey, int attempt) {
    Objects.requireNonNull(datasetKey, DATASET_KEY_CANNOT_BE_NULL);

    PipelineProcess process = mapper.getByDatasetAndAttempt(datasetKey, attempt);

    setDatasetTitle(process);

    return process;
  }

  @Override
  public PagingResponse<PipelineProcess> getRunningPipelineProcess(Pageable pageable) {
    long count = mapper.getRunningPipelineProcessCount();
    List<PipelineProcess> running = Collections.emptyList();
    if (count > 0) {
      running = mapper.getRunningPipelineProcess(pageable);
    }

    return new PagingResponse<>(pageable, count, running);
  }

  @Override
  public long createOrGet(UUID datasetKey, int attempt, String creator) {
    Objects.requireNonNull(datasetKey, DATASET_KEY_CANNOT_BE_NULL);
    Objects.requireNonNull(creator, "Creator can't be null");

    PipelineProcess pipelineProcess = new PipelineProcess();
    pipelineProcess.setDatasetKey(datasetKey);
    pipelineProcess.setAttempt(attempt);
    pipelineProcess.setCreatedBy(creator);
    mapper.createIfNotExists(pipelineProcess);

    return pipelineProcess.getKey();
  }

  @Override
  public Long getRunningExecutionKey(UUID datasetKey) {
    return mapper.getRunningExecutionKey(datasetKey);
  }

  @Transactional
  @Override
  public long addPipelineExecution(
      long pipelineProcessKey, PipelineExecution pipelineExecution, String creator) {
    Objects.requireNonNull(pipelineExecution, "pipelineExecution can't be null");
    Preconditions.checkArgument(StringUtils.isNotEmpty(creator), "creator can't be null");

    pipelineExecution.setCreatedBy(creator);

    mapper.addPipelineExecution(pipelineProcessKey, pipelineExecution);

    pipelineExecution
        .getStepsToRun()
        .forEach(
            st -> {
              PipelineStep step =
                  new PipelineStep()
                      .setType(st)
                      .setState(Status.SUBMITTED)
                      .setStarted(LocalDateTime.now())
                      .setCreatedBy(creator);

              mapper.addPipelineStep(pipelineExecution.getKey(), step);
            });

    return pipelineExecution.getKey();
  }

  @Override
  public long updatePipelineStep(PipelineStep pipelineStep, String user) {
    Objects.requireNonNull(pipelineStep, "PipelineStep can't be null");
    Preconditions.checkArgument(StringUtils.isNotEmpty(user), "user can't be null");

    pipelineStep.setStarted(LocalDateTime.now());
    pipelineStep.setCreatedBy(user);

    LOG.info("Update pipelines step: {}, type: {}, state: {}", pipelineStep.getKey(), pipelineStep.getType(),
      pipelineStep.getState());

    mapper.updatePipelineStep(pipelineStep);
    return pipelineStep.getKey();
  }

  @Override
  public List<PipelineStep> getPipelineStepsByExecutionKey(long executionKey) {
    return mapper.getPipelineStepsByExecutionKey(executionKey);
  }

  @Override
  public void markAllPipelineExecutionAsFinished() {
    mapper.markAllPipelineExecutionAsFinished();
  }

  @Override
  public void markPipelineExecutionIfFinished(long executionKey) {
    mapper.markPipelineExecutionIfFinished(executionKey);
  }

  @Override
  public void markPipelineStatusAsAborted(long executionKey) {
    mapper.markPipelineStatusAsAborted(executionKey);
  }

  @Override
  public PipelineStep getPipelineStep(long key) {
    return mapper.getPipelineStep(key);
  }

  @Override
  public PagingResponse<SearchResult> search(
      @Nullable UUID datasetKey,
      @Nullable Status state,
      @Nullable StepType stepType,
      @Nullable LocalDateTime startedMin,
      @Nullable LocalDateTime startedMax,
      @Nullable LocalDateTime finishedMin,
      @Nullable LocalDateTime finishedMax,
      @Nullable String rerunReason,
      @Nullable String pipelinesVersion,
      @Nullable Pageable page) {

    List<SearchResult> results =
        mapper.search(
            datasetKey,
            state,
            stepType,
            startedMin,
            startedMax,
            finishedMin,
            finishedMax,
            rerunReason,
            pipelinesVersion,
            page);
    long count =
        mapper.searchCount(
            datasetKey,
            state,
            stepType,
            startedMin,
            startedMax,
            finishedMin,
            finishedMax,
            rerunReason,
            pipelinesVersion);

    return new PagingResponse<>(page, count, results);
  }

  @Deprecated
  @Override
  public void sendAbsentIndentifiersEmail(UUID datasetKey, int attempt, String message) {

    try {
      String datasetName = datasetService.get(datasetKey).getTitle();

      BaseEmailModel baseEmailModel =
          pipelinesEmailManager.generateIdentifierIssueEmailModel(
              datasetKey.toString(), attempt, datasetName, message);

      LOG.info(
          "Send absent indentifiers email, datasetKey {}, attmept {}, message: {}",
          datasetKey,
          attempt,
          message);

      emailSender.send(baseEmailModel);
    } catch (IOException | TemplateException ex) {
      LOG.error(ex.getMessage(), ex);
    }
  }

  @Override
  public void notifyAbsentIdentifiers(
      UUID datasetKey, int attempt, long executionKey, String message) {
    try {
      // check if there is an open issue for this dataset
      List<IssueResult> existingIssues =
          githubApiClient.listIssues(
                  Collections.singletonList(datasetKey.toString()), "open", 1, 1);

      if (existingIssues.isEmpty()) {
        LOG.info(
            "Creating absent identifiers GH issue, datasetKey {}, attmept {}, message: {}",
            datasetKey,
            attempt,
            message);

        // create new one
        Issue issue =
            issueCreator.createIdsValidationFailedIssue(datasetKey, attempt, executionKey, message);
        githubApiClient.createIssue(issue);
      } else {
        IssueResult existing = existingIssues.get(0);

        LOG.info(
            "Updating absent identifiers GH issue with number {}, datasetKey {}, attmept {}, message: {}",
            existing.getNumber(),
            datasetKey,
            attempt,
            message);

        // add comment with new cause
        IssueComment issueComment =
            issueCreator.createIdsValidationFailedIssueComment(
                datasetKey, attempt, executionKey, message);
        githubApiClient.addIssueComment(existing.getNumber(), issueComment);

        // add labels to the existing issue
        githubApiClient.updateIssueLabels(
            existing.getNumber(),
            GithubApiClient.IssueLabels.builder()
                .labels(issueCreator.updateLabels(existing, datasetKey, attempt))
                .build());
      }
    } catch (HttpClientErrorException | HttpServerErrorException e) {
        LOG.error("Error occurred calling GitHub API: {}", e.getMessage(), e);
    } catch (Exception e) {
      LOG.error("Error in notifyAbsentIdentifiers: {}", e.getMessage(), e);
    }
  }

  @Override
  public void allowAbsentIndentifiers(UUID datasetKey, int attempt) {
    allowAbsentIndentifiersCommon(datasetKey, attempt);
  }

  @Override
  public void allowAbsentIndentifiers(UUID datasetKey) {
    allowAbsentIndentifiersCommon(datasetKey, null);
  }

  public void allowAbsentIndentifiersCommon(UUID datasetKey, Integer attempt) {
    try {
      // GET History messages
      Integer latestAttempt =
          Optional.ofNullable(attempt).orElse(mapper.getLastAttempt(datasetKey).get());
      PipelineProcess process = mapper.getByDatasetAndAttempt(datasetKey, latestAttempt);
      Optional<PipelineExecution> execution =
          process.getExecutions().stream().max(Comparator.comparingLong(PipelineExecution::getKey));

      if (execution.isPresent()) {
        PipelineExecution pipelineExecution = execution.get();
        Set<PipelineStep> steps = pipelineExecution.getSteps();
        Optional<PipelineStep> identifierStep =
            steps.stream().filter(x -> x.getType() == StepType.VERBATIM_TO_IDENTIFIER).findAny();

        if (identifierStep.isPresent() && identifierStep.get().getState() == Status.FAILED) {
          // Update and mark identier as OK
          PipelineStep pipelineStep = identifierStep.get();
          pipelineStep.setState(Status.COMPLETED);
          mapper.updatePipelineStep(pipelineStep);
          LOG.info(
              "Updated executionKey {}, datasetKey {}, attempt {} - identifier stage to completed",
              pipelineStep.getKey(),
              datasetKey,
              attempt);

          steps.stream()
              .filter(x -> x.getState() == Status.ABORTED)
              .forEach(
                  s -> {
                    PipelineStep step = s.setState(Status.SUBMITTED).setFinished(null);
                    mapper.updatePipelineStep(step);
                  });

          mapper.markPipelineExecutionAsRunning(pipelineExecution.getKey());

          // Send message to interpretaton
          PipelinesVerbatimMessage message =
              objectMapper.readValue(pipelineStep.getMessage(), PipelinesVerbatimMessage.class);
          message.getPipelineSteps().remove(StepType.ABCD_TO_VERBATIM.name());
          message.getPipelineSteps().remove(StepType.XML_TO_VERBATIM.name());
          message.getPipelineSteps().remove(StepType.DWCA_TO_VERBATIM.name());
          message.getPipelineSteps().remove(StepType.VERBATIM_TO_IDENTIFIER.name());
          String nextMessageClassName = message.getClass().getSimpleName();
          String messagePayload = message.toString();
          publisher.send(new PipelinesBalancerMessage(nextMessageClassName, messagePayload));
          LOG.info(
              "Sent MQ message to interpret dataset, executionKey {}, datasetKey {}, attempt {}",
              pipelineStep.getKey(),
              datasetKey,
              attempt);
        } else {
          LOG.warn(
              "Execution ID - {} doesn't contain failed identifier step", pipelineExecution.getKey());
        }
      }
    } catch (IOException ex) {
      LOG.error(ex.getMessage(), ex);
    }
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
    if (process == null || process.getDatasetKey() == null) {
      return;
    }

    Dataset dataset = datasetService.get(process.getDatasetKey());
    if (dataset != null) {
        process.setDatasetTitle(dataset.getTitle());
    }
  }
}
