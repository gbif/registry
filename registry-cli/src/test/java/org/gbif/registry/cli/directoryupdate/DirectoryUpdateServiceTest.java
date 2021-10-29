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
package org.gbif.registry.cli.directoryupdate;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DirectoryUpdateServiceTest {

  @Mock private ApplicationContext contextMock;
  @Mock private ScheduledExecutorService schedulerMock;
  @Mock private DirectoryUpdater directoryUpdaterMock;

  private DirectoryUpdateConfiguration config;

  @BeforeEach
  public void setUp() {
    config = new DirectoryUpdateConfiguration();
    when(contextMock.getBean(DirectoryUpdater.class)).thenReturn(directoryUpdaterMock);
  }

  @Test
  public void testStartUpAllDefaultParameters() {
    // given
    DirectoryUpdateService directoryUpdateService =
        new DirectoryUpdateService(config, contextMock, schedulerMock);

    // when
    directoryUpdateService.startUp();

    // then
    verify(schedulerMock)
        .scheduleAtFixedRate(
            any(Runnable.class),
            // no delay by default, but test may delay so accept 0 or 1 minute initial delay
            or(eq(0L), eq(1L)),
            eq(1440L),
            eq(TimeUnit.MINUTES));
  }

  @Test
  public void testStartUpCustomStartTimeAndFrequency() {
    // given
    config.startTime = LocalTime.now().plus(1L, ChronoUnit.MINUTES).toString();
    config.frequencyInHour = 12;

    DirectoryUpdateService directoryUpdateService =
        new DirectoryUpdateService(config, contextMock, schedulerMock);

    // when
    directoryUpdateService.startUp();

    // then
    verify(schedulerMock)
        .scheduleAtFixedRate(
            any(Runnable.class),
            // start time now, but test may delay so accept 0 or 1 minute initial delay
            or(eq(0L), eq(1L)),
            eq(720L),
            eq(TimeUnit.MINUTES));
  }

  @Test
  public void testStartUpStartTimePassed() {
    // given
    config.startTime = LocalTime.now().minusMinutes(1).toString();

    DirectoryUpdateService directoryUpdateService =
        new DirectoryUpdateService(config, contextMock, schedulerMock);

    // when
    directoryUpdateService.startUp();

    // then
    verify(schedulerMock)
        .scheduleAtFixedRate(
            any(Runnable.class),
            // start time passed, should be next day
            or(eq(1438L), eq(1439L)),
            eq(1440L),
            eq(TimeUnit.MINUTES));
  }
}
