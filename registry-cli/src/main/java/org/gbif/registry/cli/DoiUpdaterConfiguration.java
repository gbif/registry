package org.gbif.registry.cli;

import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.registry.cli.common.GangliaConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

public class DoiUpdaterConfiguration {

  @ParametersDelegate
  @NotNull
  @Valid
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public GangliaConfiguration ganglia = new GangliaConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public DbConfiguration db = new DbConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public DataCiteConfiguration datacite = new DataCiteConfiguration();

  @Parameter(names = "--msg-pool-size")
  @Min(1)
  public int msgPoolSize = 1;

  @Parameter(names = "--queue-name")
  @NotNull
  public String queueName;

}
