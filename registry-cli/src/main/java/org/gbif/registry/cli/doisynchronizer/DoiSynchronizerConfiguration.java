package org.gbif.registry.cli.doisynchronizer;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.User;
import org.gbif.api.service.common.UserService;
import org.gbif.common.messaging.guice.PostalServiceModule;
import org.gbif.doi.service.ServiceConfig;
import org.gbif.doi.service.datacite.DataCiteService;
import org.gbif.occurrence.query.TitleLookupModule;
import org.gbif.registry.cli.configuration.DataCiteConfiguration;
import org.gbif.registry.cli.configuration.DbConfiguration;
import org.gbif.registry.doi.DoiModule;

import java.util.Properties;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
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

  @Parameter(names = "--api-root")
  @Valid
  @NotNull
  public String apiRoot;

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

  public Injector createRegistryInjector() {
    return Guice.createInjector(db.createMyBatisModule(), new InnerRegistryModule());
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
   * Guice module for Registry related classes (except Mappers)
   */
  private class InnerRegistryModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(UserService.class).to(EmptyUserService.class);

      Properties prop = new Properties();
      prop.put("doi.prefix", DOI.GBIF_PREFIX);
      prop.put("portal.url", portalurl);
      install(new DoiModule(prop));
      install(new PostalServiceModule(PostalServiceConfiguration.SYNC_PREFIX, postalservice.toProperties()));
      install(new TitleLookupModule(true, apiRoot));
    }
  }

  /**
   * User service that will reject all authentication.
   *
   */
  private static class EmptyUserService implements UserService {

    @Nullable
    @Override
    public User authenticate(String s, String s1) {
      return null;
    }

    @Nullable
    @Override
    public User get(String s) {
      return null;
    }

    @Nullable
    @Override
    public User getBySession(String s) {
      return null;
    }
  }

}
