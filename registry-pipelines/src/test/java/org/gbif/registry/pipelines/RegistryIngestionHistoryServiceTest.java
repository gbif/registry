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

import org.gbif.api.model.crawler.CrawlJob;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.model.crawler.FinishReason;
import org.gbif.api.model.pipelines.IngestionProcess;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.DatasetProcessStatusMapper;
import org.gbif.registry.persistence.mapper.pipelines.PipelineProcessMapper;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
public class RegistryIngestionHistoryServiceTest {

  // TODO: add more tests

  @Mock private DatasetProcessStatusMapper datasetProcessStatusMapper;
  @Mock private PipelineProcessMapper pipelineProcessMapper;
  @Mock private DatasetMapper datasetMapper;
  @InjectMocks private DefaultRegistryIngestionHistoryService historyService;

  @Test
  public void notFoundIngestionProcessTest() {
    assertNull(historyService.getIngestionProcess(UUID.randomUUID(), 1));
  }

  @Test
  public void getIngestionProcessTest() {
    UUID datasetKey = UUID.randomUUID();
    int attempt = 1;

    DatasetProcessStatus status = createDatasetProcessStatus(datasetKey, attempt);
    Mockito.when(datasetProcessStatusMapper.get(datasetKey, attempt)).thenReturn(status);

    PipelineProcess process = createPipelineProcess(datasetKey, attempt);

    Mockito.when(pipelineProcessMapper.getByDatasetAndAttempt(datasetKey, attempt))
        .thenReturn(process);

    Dataset dataset = createDataset(datasetKey);
    Mockito.when(datasetMapper.get(dataset.getKey())).thenReturn(dataset);

    IngestionProcess ingestionProcess = historyService.getIngestionProcess(datasetKey, attempt);

    assertEquals(datasetKey, ingestionProcess.getDatasetKey());
    assertEquals(attempt, ingestionProcess.getAttempt());
    assertEquals(status, ingestionProcess.getCrawlInfo());
    assertEquals(process.getExecutions(), ingestionProcess.getPipelineExecutions());
    assertEquals(dataset.getTitle(), ingestionProcess.getDatasetTitle());
  }

  private DatasetProcessStatus createDatasetProcessStatus(UUID datasetKey, int attempt) {
    DatasetProcessStatus status = new DatasetProcessStatus();
    status.setDatasetKey(datasetKey);
    status.setFinishReason(FinishReason.NORMAL);
    status.setPagesCrawled(10);
    CrawlJob crawlJob =
        new CrawlJob(
            status.getDatasetKey(),
            attempt,
            EndpointType.DWC_ARCHIVE,
            URI.create("http://test.com"));
    status.setCrawlJob(crawlJob);

    return status;
  }

  private PipelineProcess createPipelineProcess(UUID datasetKey, int attempt) {
    PipelineStep step =
        new PipelineStep().setStarted(LocalDateTime.now()).setType(StepType.ABCD_TO_VERBATIM);

    PipelineExecution execution = new PipelineExecution();
    execution.setSteps(Collections.singleton(step));

    PipelineProcess process = new PipelineProcess().setDatasetKey(datasetKey).setAttempt(attempt);
    process.setExecutions(Collections.singleton(execution));

    return process;
  }

  private Dataset createDataset(UUID key) {
    Dataset dataset = new Dataset();
    dataset.setKey(key);
    dataset.setTitle("title");
    return dataset;
  }
}
