package org.gbif.registry.cli.common;

import org.gbif.directory.client.guice.DirectoryWsClientModule;

import java.util.Properties;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;

/**
 * Holds configuration for the registry or drupal database.
 */
@SuppressWarnings("PublicField")
public class DirectoryConfiguration {

  @NotNull
  @Parameter(names = "--directory-ws-url")
  public String wsUrl;

  @NotNull
  @Parameter(names = "--directory-app-key")
  public String appKey;

  @NotNull
  @Parameter(names = "--directory-app-secret")
  public String appSecret;

  /**
   * Create a Properties object from the public fields from that class and its included db.
   */
  public Properties toProperties(){
    Properties props = new Properties();
    props.put(DirectoryWsClientModule.DIRECTORY_URL_KEY, wsUrl);
    props.put(DirectoryWsClientModule.DIRECTORY_APP_KEY, appKey);
    props.put(DirectoryWsClientModule.DIRECTORY_SECRET, appSecret);
    return props;
  }

}
