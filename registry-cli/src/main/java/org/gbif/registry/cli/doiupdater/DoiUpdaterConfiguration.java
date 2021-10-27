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
package org.gbif.registry.cli.doiupdater;

import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.registry.cli.common.DataCiteConfiguration;
import org.gbif.registry.cli.common.DbConfiguration;
import org.gbif.registry.cli.common.GangliaConfiguration;

import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

public class DoiUpdaterConfiguration {

  @ParametersDelegate @NotNull @Valid
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @ParametersDelegate @Valid @NotNull
  public GangliaConfiguration ganglia = new GangliaConfiguration();

  @ParametersDelegate @Valid @NotNull public DbConfiguration registry = new DbConfiguration();

  @ParametersDelegate @Valid @NotNull
  public DataCiteConfiguration datacite = new DataCiteConfiguration();

  @Parameter(names = "--queue-name")
  @NotNull
  public String queueName;

  @Parameter(names = "--retry-time")
  public long timeToRetryInMs = TimeUnit.MINUTES.toMillis(5);

  @Override
  public String toString() {
    return new StringJoiner(", ", DoiUpdaterConfiguration.class.getSimpleName() + "[", "]")
        .add("messaging=" + messaging)
        .add("ganglia=" + ganglia)
        .add("registry=" + registry)
        .add("datacite=" + datacite)
        .add("queueName='" + queueName + "'")
        .add("timeToRetryInMs=" + timeToRetryInMs)
        .toString();
  }
}
