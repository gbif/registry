package org.gbif.registry.persistence.guice;

import org.gbif.doi.service.DoiService;
import org.gbif.doi.service.ServiceConfig;
import org.gbif.doi.service.datacite.DataciteService;
import org.gbif.utils.HttpUtil;

import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

/**
 * A Doi Service module implementing a datacite bound DOI service.
 */
public class DataCiteModule extends AbstractModule {
  private final Properties properties;

  public DataCiteModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    ServiceConfig cfg = new ServiceConfig(
      properties.getProperty("doi.username"),
      properties.getProperty("doi.password")
    );
    bind(DoiService.class).toInstance(new DataciteService(HttpUtil.newMultithreadedClient(10000, 20, 10), cfg));
    bind(String.class).annotatedWith(Names.named("doi.prefix")).toInstance(properties.getProperty("doi.prefix"));
  }
}
