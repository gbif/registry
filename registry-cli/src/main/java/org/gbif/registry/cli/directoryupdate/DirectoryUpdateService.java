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

import org.gbif.registry.cli.common.spring.SpringContextBuilder;
import org.gbif.registry.service.WithMyBatis;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.google.common.util.concurrent.AbstractIdleService;

/**
 * Service that will periodically get the list of participants and nodes from the Directory and
 * update the registry data if required. This service will NOT remove nodes that are not available
 * in the Directory.
 */
@SuppressWarnings("UnstableApiUsage")
public class DirectoryUpdateService extends AbstractIdleService {

  private static final Logger LOG = LoggerFactory.getLogger(DirectoryUpdateService.class);

  // avoid using 0
  private static final int DEFAULT_START_HOUR = 5;
  private static final int DEFAULT_START_MINUTE = 34;
  private static final int DEFAULT_FREQUENCY = 24;

  private Integer frequencyInHour;
  private Integer startHour;
  private Integer startMinute;

  private ScheduledExecutorService scheduler;
  private DirectoryUpdater directoryUpdater;
  private ApplicationContext context;

  public DirectoryUpdateService(DirectoryUpdateConfiguration config) {
    this(
        config,
        SpringContextBuilder.create()
            .withDbConfiguration(config.db)
            .withDirectoryConfiguration(config.directory)
            .withComponents(DirectoryUpdater.class, WithMyBatis.class)
            .build(),
        Executors.newScheduledThreadPool(1));
  }

  // separate method in order to have an option to configure context manually
  public DirectoryUpdateService(
      DirectoryUpdateConfiguration config,
      ApplicationContext context,
      ScheduledExecutorService scheduler) {
    this.context = context;
    this.scheduler = scheduler;
    this.directoryUpdater = context.getBean(DirectoryUpdater.class);
    this.frequencyInHour = ObjectUtils.defaultIfNull(config.frequencyInHour, DEFAULT_FREQUENCY);

    if (StringUtils.contains(config.startTime, ":")) {
      String[] timeParts = config.startTime.split(":");
      this.startHour = NumberUtils.toInt(timeParts[0], DEFAULT_START_HOUR);
      this.startMinute = NumberUtils.toInt(timeParts[1], DEFAULT_START_MINUTE);
    } else {
      this.startHour = null;
      this.startMinute = null;
    }
  }

  @Override
  protected void startUp() {
    long initialDelay = 0;
    if (startHour != null && startMinute != null) {
      LocalTime t = LocalTime.of(startHour, startMinute);
      initialDelay = LocalTime.now().until(t, ChronoUnit.MINUTES);
    }

    // if the delay is passed
    if (initialDelay < 0) {
      initialDelay = initialDelay + ChronoUnit.DAYS.getDuration().toMinutes();
    }

    LOG.info("DirectoryUpdateService Starting in {} minute(s)", initialDelay);

    scheduler.scheduleAtFixedRate(
        () -> directoryUpdater.applyUpdates(),
        initialDelay,
        frequencyInHour * (ChronoUnit.HOURS.getDuration().toMinutes()),
        TimeUnit.MINUTES);
  }

  @Override
  protected void shutDown() {
    scheduler.shutdown();
  }

  public ApplicationContext getContext() {
    return context;
  }
}
