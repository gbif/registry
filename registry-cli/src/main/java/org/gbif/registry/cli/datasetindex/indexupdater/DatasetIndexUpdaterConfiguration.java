package org.gbif.registry.cli.datasetindex.indexupdater;

import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.registry.cli.common.DbConfiguration;
import org.gbif.registry.cli.datasetindex.DatasetIndexConfiguration;
import org.gbif.registry.cli.datasetindex.ElasticsearchConfig;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** Configuration required to start Indexing Pipeline on provided dataset */
@Data
@EqualsAndHashCode(callSuper = true)
public class DatasetIndexUpdaterConfiguration extends DatasetIndexConfiguration {

  @ParametersDelegate @Valid @NotNull
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @Parameter(names = "--queue-name")
  @NotNull
  public String queueName;

  @Parameter(names = "--pool-size")
  @NotNull
  @Min(1)
  public int poolSize;
}
