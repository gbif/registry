package org.gbif.registry.pipelines.guice;

import org.gbif.registry.pipelines.*;
import org.gbif.service.guice.PrivateServiceModule;

import java.util.Properties;

import com.google.inject.Scopes;

public class PipelinesModule extends PrivateServiceModule {

  private static final String PREFIX = "pipelines.";

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
    bind(MetricsHandler.class).asEagerSingleton();

    expose(PipelinesHistoryTrackingService.class);
    expose(IngestionHistoryService.class);
    expose(MetricsHandler.class);
  }
}
