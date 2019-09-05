package org.gbif.registry.pipelines.guice;

import org.gbif.registry.pipelines.PipelinesCoordinatorTrackingServiceImpl;
import org.gbif.registry.pipelines.PipelinesHistoryTrackingService;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;

public class PipelinesModule extends PrivateModule {

  @Override
  protected void configure() {
    bind(PipelinesHistoryTrackingService.class)
        .to(PipelinesCoordinatorTrackingServiceImpl.class)
        .in(Scopes.SINGLETON);
    expose(PipelinesHistoryTrackingService.class);
  }
}
