package org.gbif.registry.cli.datasetindex;

import org.gbif.common.search.solr.SolrConfig;
import org.gbif.registry.cli.common.DbConfiguration;
import org.gbif.registry.cli.common.DirectoryConfiguration;

import java.util.Properties;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import static org.gbif.registry.search.guice.RegistrySearchModule.INDEXING_THREADS_PROP;

/**
 *
 */
public class IndexBuilderConfig {

  @ParametersDelegate
  @Valid
  @NotNull
  public DbConfiguration registry = new DbConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public SolrConfig solr= new SolrConfig();

  @ParametersDelegate
  @Valid
  @NotNull
  public DirectoryConfiguration directory = new DirectoryConfiguration();

  @Parameter(names = "--api-root")
  @Valid
  @NotNull
  public String apiRoot;

  @Parameter(names = "--indexing-threads")
  @Min(1)
  @Max(10)
  public int indexingThreads = 2;

  public Properties toProperties(){
    Properties props = registry.toRegistryProperties();
    props.putAll(directory.toProperties());
    props.putAll(solr.toProperties("solr.dataset."));
    props.put("api.url", apiRoot);
    props.put(INDEXING_THREADS_PROP, indexingThreads);
    return props;
  }

}
