package org.gbif.registry.cli.indexbuilder;

import org.gbif.common.search.solr.SolrConfig;
import org.gbif.registry.cli.common.DbConfiguration;
import org.gbif.registry.cli.common.DirectoryConfiguration;

import java.util.Properties;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

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

  public Properties toProperties(){
    Properties props = registry.toRegistryProperties();
    props.putAll(directory.toProperties());
    props.putAll(solr.toProperties("solr.dataset."));
    props.put("api.url", apiRoot);
    return props;
  }

}
