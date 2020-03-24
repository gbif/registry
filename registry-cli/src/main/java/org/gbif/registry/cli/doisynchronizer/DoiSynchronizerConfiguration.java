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

import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.registry.cli.common.DataCiteConfiguration;
import org.gbif.registry.cli.common.DbConfiguration;

import java.util.StringJoiner;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

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
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @Parameter(names = "--doi")
  @NotNull
  public String doi = "";

  @Parameter(names = "--doi-list")
  @NotNull
  public String doiList = "";

  @Parameter(names = {"--fix-doi"})
  @Valid
  public boolean fixDOI = false;

  @Parameter(names = {"--skip-dia"})
  @Valid
  public boolean skipDiagnostic = false;

  @Parameter(names = {"--export"})
  @Valid
  public boolean export = false;

  @Parameter(names = {"--list-failed-doi"})
  @Valid
  public boolean listFailedDOI = false;

  @Override
  public String toString() {
    return new StringJoiner(", ", DoiSynchronizerConfiguration.class.getSimpleName() + "[", "]")
        .add("portalurl='" + portalurl + "'")
        .add("apiRoot='" + apiRoot + "'")
        .add("registry=" + registry)
        .add("datacite=" + datacite)
        .add("messaging=" + messaging)
        .add("doi='" + doi + "'")
        .add("doiList='" + doiList + "'")
        .add("fixDOI=" + fixDOI)
        .add("skipDiagnostic=" + skipDiagnostic)
        .add("export=" + export)
        .add("listFailedDOI=" + listFailedDOI)
        .toString();
  }
}
