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
package org.gbif.registry.cli.doisynchronizer;

import org.gbif.api.model.common.DoiStatus;
import org.gbif.doi.service.DoiService;
import org.gbif.registry.doi.DatasetDoiDataCiteHandlingService;
import org.gbif.registry.doi.DoiIssuingService;
import org.gbif.registry.doi.DownloadDoiDataCiteHandlingService;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.DoiMapper;
import org.gbif.registry.persistence.mapper.OccurrenceDownloadMapper;
import org.gbif.registry.persistence.mapper.UserMapper;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.ImmutableMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DoiSynchronizerTest {

  @Mock private ApplicationContext contextMock;
  @Mock private DoiMapper doiMapperMock;
  @Mock private DoiIssuingService doiIssuingServiceMock;
  @Mock private DatasetDoiDataCiteHandlingService datasetDoiDataCiteHandlingServiceMock;
  @Mock private DownloadDoiDataCiteHandlingService downloadDoiDataCiteHandlingServiceMock;
  @Mock private DatasetMapper datasetMapperMock;
  @Mock private OccurrenceDownloadMapper occurrenceDownloadMapperMock;
  @Mock private UserMapper userMapperMock;
  @Mock private DoiService doiServiceMock;

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;

  @BeforeEach
  public void setUpStreams() {
    System.setOut(new PrintStream(outContent));
  }

  @AfterEach
  public void restoreStreams() {
    System.setOut(originalOut);
  }

  @BeforeEach
  public void before() {
    when(contextMock.getBean(DoiMapper.class)).thenReturn(doiMapperMock);
    when(contextMock.getBean(DoiIssuingService.class)).thenReturn(doiIssuingServiceMock);
    when(contextMock.getBean(DatasetDoiDataCiteHandlingService.class))
        .thenReturn(datasetDoiDataCiteHandlingServiceMock);
    when(contextMock.getBean(DownloadDoiDataCiteHandlingService.class))
        .thenReturn(downloadDoiDataCiteHandlingServiceMock);
    when(contextMock.getBean(DatasetMapper.class)).thenReturn(datasetMapperMock);
    when(contextMock.getBean(OccurrenceDownloadMapper.class))
        .thenReturn(occurrenceDownloadMapperMock);
    when(contextMock.getBean(UserMapper.class)).thenReturn(userMapperMock);
    when(contextMock.getBean(DoiService.class)).thenReturn(doiServiceMock);
  }

  @Test
  public void testPrintFailed() {
    // given
    Map<String, Object> datasetDoi1 =
        ImmutableMap.of(
            "doi", "10.21373/1000",
            "status", "FAILED",
            "type", "DATASET");
    Map<String, Object> downloadDoi1 =
        ImmutableMap.of(
            "doi", "10.21373/1001",
            "status", "FAILED",
            "type", "DOWNLOAD");

    when(doiMapperMock.list(DoiStatus.FAILED, null, null))
        .thenReturn(Arrays.asList(datasetDoi1, downloadDoi1));

    DoiSynchronizerConfiguration configuration = new DoiSynchronizerConfiguration();
    configuration.listFailedDOI = true;

    DoiSynchronizer doiSynchronizer = new DoiSynchronizer(configuration, contextMock);

    // when
    doiSynchronizer.printFailedDOI();

    // then
    verify(doiMapperMock, atLeastOnce()).list(DoiStatus.FAILED, null, null);
    assertEquals("10.21373/1000 (DATASET)\n" + "10.21373/1001 (DOWNLOAD)\n", outContent.toString());
  }
}
