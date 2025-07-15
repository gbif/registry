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
package org.gbif.registry.cli.vocabularysynchronizer;

import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.registry.cli.common.DbConfiguration;
import org.gbif.registry.cli.datasetindex.ElasticsearchConfig;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class VocabularySynchronizerConfiguration {

  @ParametersDelegate @Valid @NotNull
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @Parameter(
      names = "--queue-name",
      description = "The name of the queue to listen to for vocabulary released messages")
  @NotNull
  public String queueName = "vocabulary-released";

  @Parameter(names = "--pool-size", description = "The size of the thread pool to listen to messages")
  public int poolSize = 1;

  @Setter
  @Getter
  @ParametersDelegate @Valid @NotNull
  private DbConfiguration dbConfig;

  @Setter
  @Getter
  @JsonProperty("apiRootUrl")
  @NotNull
  public String apiRootUrl;

  @ParametersDelegate @Valid @NotNull
  private ElasticsearchConfig elasticsearch = new ElasticsearchConfig();

  @JsonProperty("vocabulariesToProcess")
  @NotNull
  public Set<String> vocabulariesToProcess;

  @Override
  public String toString() {
    return "VocabularySynchronizerConfiguration{" +
        "messaging=" + messaging +
        ", queueName='" + queueName + '\'' +
        ", poolSize=" + poolSize +
        ", dbConfig=" + dbConfig +
        ", apiRootUrl='" + apiRootUrl + '\'' +
        ", elasticsearch=" + elasticsearch +
        ", vocabulariesToProcess=" + vocabulariesToProcess +
        '}';
  }
}