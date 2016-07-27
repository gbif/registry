package org.gbif.registry.cli.directoryupdate;

import org.gbif.directory.client.guice.DirectoryWsClientModule;
import org.gbif.registry.persistence.guice.RegistryMyBatisModule;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.LocalTime;
import org.threeten.bp.temporal.ChronoUnit;

/**
 *
 * Service that will periodically get the list of participants and nodes from the Directory and update the registry
 * data if required.
 * This service will NOT remove nodes that are not available in the Directory.
 *
 */
public class DirectoryUpdateService extends AbstractIdleService {

  private static final Logger LOG = LoggerFactory.getLogger(DirectoryUpdateService.class);

  //avoid using 0
  private static final int DEFAULT_START_HOUR = 5;
  private static final int DEFAULT_START_MINUTE = 34;
  private static final int DEFAULT_FREQUENCY = 24;

  private final Integer frequencyInHour;
  private final Integer startHour;
  private final Integer startMinute;

  private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private DirectoryUpdater directoryUpdater;

  public DirectoryUpdateService(DirectoryUpdateConfiguration cfg) {
    this(cfg,
            Guice.createInjector(
                    new DirectoryWsClientModule(cfg.toProperties()),
                    new RegistryMyBatisModule(cfg.db.toRegistryProperties())
            )
    );
  }

  public DirectoryUpdateService(DirectoryUpdateConfiguration cfg, Injector injector) {

    directoryUpdater = injector.getInstance(DirectoryUpdater.class);

    this.frequencyInHour = ObjectUtils.defaultIfNull(cfg.frequencyInHour, DEFAULT_FREQUENCY);

    if(StringUtils.contains(cfg.startTime, ":")) {
      String[] timeParts = cfg.startTime.split(":");
      startHour = NumberUtils.toInt(timeParts[0], DEFAULT_START_HOUR);
      startMinute = NumberUtils.toInt(timeParts[1], DEFAULT_START_MINUTE);
    }
    else{
      startHour = null;
      startMinute = null;
    }
  }

  @Override
  protected void startUp() throws Exception {
    long initialDelay = 0;
    if(startHour != null && startMinute != null) {
      LocalTime t = LocalTime.of(startHour, startMinute);
      initialDelay = LocalTime.now().until(t, ChronoUnit.MINUTES);
    }

    //if the delay is passed,
    if (initialDelay < 0) {
      initialDelay = initialDelay + ChronoUnit.DAYS.getDuration().toMinutes();
    }

    LOG.info("DirectoryUpdateService Starting in " + initialDelay + " minute(s)");

    scheduler.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        directoryUpdater.applyUpdates();
      }
    }, initialDelay, frequencyInHour * (ChronoUnit.MINUTES.getDuration().getSeconds()), TimeUnit.MINUTES);
  }

  @Override
  protected void shutDown() throws Exception {
    scheduler.shutdown();
  }

}
