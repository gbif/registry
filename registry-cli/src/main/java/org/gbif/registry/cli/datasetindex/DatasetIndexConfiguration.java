package org.gbif.registry.cli.datasetindex;

import org.gbif.registry.cli.common.DbConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import lombok.Data;

@Data
public class DatasetIndexConfiguration {

  @Parameter(names = "--api-root-url")
  @NotNull
  private String apiRootUrl;

  @Parameter(names = "--registry-ws-url")
  private String registryWsUrl;

  @ParametersDelegate @Valid @NotNull private DbConfiguration clbDb;

  @ParametersDelegate @Valid @NotNull private ElasticsearchConfig datasetEs;

  @ParametersDelegate @Valid @NotNull private ElasticsearchConfig occurrenceEs;

  private boolean indexClb = true;

  private Integer stopAfter = -1;

  private Integer pageSize = 50;
}
