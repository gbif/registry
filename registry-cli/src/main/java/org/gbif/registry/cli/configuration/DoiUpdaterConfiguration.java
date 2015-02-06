package org.gbif.registry.cli.configuration;

import org.gbif.common.messaging.config.MessagingConfiguration;

import java.util.concurrent.TimeUnit;
import javax.validation.Valid;
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

  @Parameter(names = "--queue-name")
  @NotNull
  public String queueName;

  @Parameter(names = "--retry-time")
  public long timeToRetryInMs = TimeUnit.MINUTES.toMillis(5);
}
