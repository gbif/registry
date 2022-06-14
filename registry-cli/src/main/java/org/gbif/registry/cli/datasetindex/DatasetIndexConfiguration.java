/*
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
