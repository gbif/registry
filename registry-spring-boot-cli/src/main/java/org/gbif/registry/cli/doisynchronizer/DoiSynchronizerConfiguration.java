/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.cli.doisynchronizer;

import org.gbif.registry.cli.common.DataCiteConfiguration;
import org.gbif.registry.cli.common.DbConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/** */
public class DoiSynchronizerConfiguration {

  @Parameter(names = "--portal-url")
  @Valid
  @NotNull
  public String portalurl;

  @Parameter(names = "--api-root")
  @Valid
  @NotNull
  public String apiRoot;

  @Valid @NotNull public DbConfiguration registry = new DbConfiguration();

  @ParametersDelegate @Valid @NotNull
  public DataCiteConfiguration datacite = new DataCiteConfiguration();

  @ParametersDelegate @Valid @NotNull
  public PostalServiceConfiguration postalservice = new PostalServiceConfiguration();

  @Parameter(names = "--doi", required = false)
  @NotNull
  public String doi = "";

  @Parameter(names = "--doi-list", required = false)
  @NotNull
  public String doiList = "";

  @Parameter(
      names = {"--fix-doi"},
      required = false)
  @Valid
  public boolean fixDOI = false;

  @Parameter(
      names = {"--skip-dia"},
      required = false)
  @Valid
  public boolean skipDiagnostic = false;

  @Parameter(
      names = {"--export"},
      required = false)
  @Valid
  public boolean export = false;

  @Parameter(
      names = {"--list-failed-doi"},
      required = false)
  @Valid
  public boolean listFailedDOI = false;
}
