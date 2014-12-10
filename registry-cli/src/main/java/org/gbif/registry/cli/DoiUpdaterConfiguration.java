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

  @Parameter(names = "--msg-pool-size")
  @Min(1)
  public int msgPoolSize = 1;

  @Parameter(names = "--queue-name")
  @NotNull
  public String queueName;

  @Parameter(names = "--doi-username")
  @NotNull
  public String doiUsername;

  @Parameter(names = "--doi-password")
  @NotNull
  public String doiPassword;

  @Parameter(names = "--db-host")
  @NotNull
  public String dbHost;

  @Parameter(names = "--db-name")
  @NotNull
  public String dbName;

  @Parameter(names = "--db-username")
  @NotNull
  public String dbUsername;

  @Parameter(names = "--db-password")
  @NotNull
  public String dbPassword;
}
