package org.gbif.registry.pipelines;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import static org.gbif.registry.pipelines.guice.PipelinesModule.THREAD_POOL_SIZE;

public class PipelinesMockModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(PipelinesHistoryTrackingService.class)
        .to(DefaultPipelinesHistoryTrackingService.class)
        .in(Scopes.SINGLETON);
    bind(IngestionHistoryService.class)
        .to(DefaultIngestionHistoryService.class)
        .in(Scopes.SINGLETON);
    bind(new TypeLiteral<ExecutorService>() {})
      .annotatedWith(Names.named(THREAD_POOL_SIZE))
      .toInstance(Executors.newSingleThreadExecutor());
  }
}
