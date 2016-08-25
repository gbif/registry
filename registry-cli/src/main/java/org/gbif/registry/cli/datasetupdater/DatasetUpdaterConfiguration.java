package org.gbif.registry.cli.datasetupdater;

import org.gbif.registry.cli.common.DbConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 * A configuration exclusively for DatasetUpdater.
 */
public class DatasetUpdaterConfiguration {

  @ParametersDelegate
  @Valid
  @NotNull
  public DbConfiguration db = new DbConfiguration();

  @Parameter(names = "--dataset-key")
  public String key;

  @Parameter(names = "--dataset-key-path")
  public String keyFilePath;
}
