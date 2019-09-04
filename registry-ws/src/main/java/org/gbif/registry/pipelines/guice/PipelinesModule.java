package org.gbif.registry.pipelines.guice;

import org.gbif.common.messaging.guice.PostalServiceModule;
import org.gbif.registry.pipelines.PipelinesCoordinatorTrackingServiceImpl;
import org.gbif.registry.pipelines.PipelinesHistoryTrackingService;

import java.util.Properties;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;

import static org.gbif.registry.events.EventModule.MESSAGING_ENABLED_PROPERTY;

public class PipelinesModule extends PrivateModule {

  private final Properties properties;

  public PipelinesModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
//    if (properties.getProperty(MESSAGING_ENABLED_PROPERTY, "false").equals("true")) {
//      install(new PostalServiceModule("registry", properties));
//    }

    bind(PipelinesHistoryTrackingService.class)
        .to(PipelinesCoordinatorTrackingServiceImpl.class)
        .in(Scopes.SINGLETON);
    expose(PipelinesHistoryTrackingService.class);
  }
}
