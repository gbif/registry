package org.gbif.registry.cli;

import org.gbif.doi.service.ServiceConfig;
import org.gbif.doi.service.datacite.DataCiteService;
import org.gbif.utils.HttpUtil;

import java.net.URI;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A configuration for the DataCite service.
 */
@SuppressWarnings("PublicField")
public class DataCiteConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(DataCiteConfiguration.class);

  @Parameter(names = "--datacite-username")
  @NotNull
  public String username;

  @Parameter(names = "--datacite-password")
  @NotNull
  public String password;

  @Parameter(names = "--datacite-api")
  @Nullable
  public URI api;

  @Parameter(names = "--datacite-threads")
  public int threads = 10;

  @Parameter(names = "--datacite-timeout")
  public int timeout = 20000;

  public DataCiteService createService() {
    LOG.debug("Creating DataCite doi service");
    ServiceConfig cfg = new ServiceConfig(username, password);
    cfg.setApi(api);
    return new DataCiteService(HttpUtil.newMultithreadedClient(timeout, threads, threads), cfg);
  }

}
