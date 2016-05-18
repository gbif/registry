package org.gbif.registry.cli.doisynchronizer;

import org.gbif.api.model.common.DOI;
import org.gbif.common.messaging.guice.PostalServiceModule;
import org.gbif.doi.service.ServiceConfig;
import org.gbif.doi.service.datacite.DataCiteService;
import org.gbif.registry.cli.configuration.DataCiteConfiguration;
import org.gbif.registry.cli.configuration.DbConfiguration;
import org.gbif.registry.doi.DoiPersistenceService;
import org.gbif.registry.doi.generator.DoiGenerator;
import org.gbif.registry.doi.generator.DoiGeneratorMQ;
import org.gbif.registry.persistence.mapper.DoiMapper;

import java.net.URI;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 */
public class DoiSynchronizerConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(DataCiteConfiguration.class);

  @Parameter(names = "--portal-url")
  @Valid
  @NotNull
  public String portalurl;

  @Parameter(names = {"--print-report"}, required = false)
  @Valid
  public boolean printReport = true;

  @Parameter(names = {"--fix-doi"}, required = false)
  @Valid
  public boolean fixDOI = false;

  @ParametersDelegate
  @Valid
  @NotNull
  public DbConfiguration db = new DbConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public DataCiteConfiguration datacite = new DataCiteConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public PostalServiceConfiguration postalservice = new PostalServiceConfiguration();

  public Injector createMyBatisInjector() {
    return Guice.createInjector(db.createMyBatisModule(), new DoiModule());
  }

  public DataCiteService createDataCiteService() {
    LOG.debug("Creating DataCite doi service");
    ServiceConfig cfg = new ServiceConfig(datacite.username, datacite.password);
    cfg.setApi(datacite.api);

    RequestConfig.Builder requestBuilder = RequestConfig.custom();
    requestBuilder = requestBuilder.setConnectTimeout(datacite.timeout);
    requestBuilder = requestBuilder.setConnectionRequestTimeout(datacite.timeout);

    HttpClientBuilder builder = HttpClientBuilder.create().setDefaultRequestConfig(requestBuilder.build());

    return new DataCiteService(builder.build(), cfg);
  }

  /**
   * Guice module for DOI
   */
  private class DoiModule  extends AbstractModule {

    @Override
    protected void configure() {
      bind(DoiGenerator.class).to(DoiGeneratorMQ.class).in(Scopes.SINGLETON);
      bind(DoiPersistenceService.class).to(DoiMapper.class).in(Scopes.SINGLETON);

      bind(String.class).annotatedWith(Names.named("doi.prefix")).toInstance(DOI.GBIF_PREFIX);
      bind(URI.class).annotatedWith(Names.named("portal.url")).toInstance(URI.create(portalurl));

      install(new PostalServiceModule(PostalServiceConfiguration.SYNC_PREFIX, postalservice.toProperties()));
    }
  }

}
