package org.gbif.registry.cli.directoryupdate;

import org.gbif.directory.client.guice.DirectoryWsClientModule;
import org.gbif.registry.cli.common.DbConfiguration;

import java.util.Properties;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 *
 */
public class DirectoryUpdateConfiguration {

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

  public Properties toProperties(){
    Properties props = new Properties();
    props.put(DirectoryWsClientModule.DIRECTORY_URL_KEY, directoryWsUrl);
    props.put(DirectoryWsClientModule.DIRECTORY_APP_KEY, directoryAppKey);
    props.put(DirectoryWsClientModule.DIRECTORY_SECRET, directoryAppSecret);
    return props;
  }

}
