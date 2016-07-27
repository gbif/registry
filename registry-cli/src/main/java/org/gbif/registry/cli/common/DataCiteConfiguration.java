package org.gbif.registry.cli.common;

import java.net.URI;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Nullable;

/**
 * A configuration for the DataCite service.
 */
@SuppressWarnings("PublicField")
public class DataCiteConfiguration {
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

}
