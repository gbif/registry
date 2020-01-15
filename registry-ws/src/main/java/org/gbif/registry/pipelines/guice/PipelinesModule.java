package org.gbif.registry.pipelines.guice;

import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.gbif.registry.pipelines.DefaultIngestionHistoryService;
import org.gbif.registry.pipelines.DefaultPipelinesHistoryTrackingService;
import org.gbif.registry.pipelines.IngestionHistoryService;
import org.gbif.registry.pipelines.PipelinesHistoryTrackingService;
import org.gbif.service.guice.PrivateServiceModule;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class PipelinesModule extends PrivateServiceModule {

  private static final String PREFIX = "pipelines.";

  public static final String THREAD_POOL_SIZE = PREFIX + "do.all.threads";

  public PipelinesModule(Properties properties) {
    super(PREFIX, properties);
  }

  @Override
  protected void configureService() {
    bind(PipelinesHistoryTrackingService.class)
        .to(DefaultPipelinesHistoryTrackingService.class)
        .in(Scopes.SINGLETON);
    bind(IngestionHistoryService.class)
        .to(DefaultIngestionHistoryService.class)
        .in(Scopes.SINGLETON);

    expose(PipelinesHistoryTrackingService.class);
    expose(IngestionHistoryService.class);
  }

  /** To use several threads when we run doAll from history and send messages in queues  */
  @Provides
  @Singleton
  private ExecutorService provideExecutorService(@Named(THREAD_POOL_SIZE) Integer threadPoolSize) {
    return Optional.ofNullable(threadPoolSize)
      .map(Executors::newFixedThreadPool)
      .orElse(Executors.newSingleThreadExecutor());
  }
}
