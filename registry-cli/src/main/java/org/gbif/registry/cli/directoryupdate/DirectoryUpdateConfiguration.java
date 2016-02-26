package org.gbif.registry.cli.directoryupdate;

import org.gbif.directory.client.guice.DirectoryWsClientModule;
import org.gbif.registry.cli.configuration.DbConfiguration;

import java.util.Properties;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class DirectoryUpdateConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(DirectoryUpdateConfiguration.class);

  @ParametersDelegate
  @Valid
  @NotNull
  public DbConfiguration db = new DbConfiguration();

  @NotNull
  @Parameter(names = "--directory-ws-url")
  public String directoryWsUrl;

  @NotNull
  @Parameter(names = "--directory-app-key")
  public String directoryAppKey;

  @NotNull
  @Parameter(names = "--directory-app-secret")
  public String directoryAppSecret;

  @NotNull
  @Parameter(names = "--start-time")
  public String startTime;

  @NotNull
  @Parameter(names = "--frequency-in-hour")
  public Integer frequencyInHour = 24;

  public Injector createInjector() {
    Properties props = new Properties();
    props.put(DirectoryWsClientModule.DIRECTORY_URL_KEY, directoryWsUrl);
    props.put(DirectoryWsClientModule.DIRECTORY_APP_KEY, directoryAppKey);
    props.put(DirectoryWsClientModule.DIRECTORY_SECRET, directoryAppSecret);

    Injector injClient = Guice.createInjector(new DirectoryWsClientModule(props));
    LOG.info("Connecting to Directory services at {}", directoryWsUrl);
    return injClient;
  }

  public Injector createMyBatisInjector() {
    return Guice.createInjector(db.createMyBatisModule());
  }

}
