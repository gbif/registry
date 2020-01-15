package org.gbif.registry.pipelines;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class PipelinesMockModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ExecutorService.class)
      .toInstance(Executors.newSingleThreadExecutor());
    bind(PipelinesHistoryTrackingService.class)
        .to(DefaultPipelinesHistoryTrackingService.class)
        .in(Scopes.SINGLETON);
    bind(IngestionHistoryService.class)
        .to(DefaultIngestionHistoryService.class)
        .in(Scopes.SINGLETON);
  }
}
