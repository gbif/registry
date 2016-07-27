package org.gbif.registry.cli.doisynchronizer;

import org.gbif.registry.cli.common.DataCiteConfiguration;
import org.gbif.registry.cli.common.DbConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 *
 *
 */
public class DoiSynchronizerConfiguration {

  @Parameter(names = "--portal-url")
  @Valid
  @NotNull
  public String portalurl;

  @Parameter(names = "--api-root")
  @Valid
  @NotNull
  public String apiRoot;

  @Parameter(names = {"--print-report"}, required = false)
  @Valid
  public boolean printReport = true;

  @Parameter(names = {"--fix-doi"}, required = false)
  @Valid
  public boolean fixDOI = false;

  @Valid
  @NotNull
  public DbConfiguration registry = new DbConfiguration();

  @Valid
  @NotNull
  public DbConfiguration drupal = new DbConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public DataCiteConfiguration datacite = new DataCiteConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public PostalServiceConfiguration postalservice = new PostalServiceConfiguration();


  @Parameter(names = "--doi", required = false)
  @NotNull
  public String doi = "";


}
