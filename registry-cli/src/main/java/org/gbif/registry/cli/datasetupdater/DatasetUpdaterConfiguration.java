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
package org.gbif.registry.cli.datasetupdater;

import org.gbif.registry.cli.common.DbConfiguration;

import java.util.StringJoiner;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/** A configuration exclusively for DatasetUpdater. */
public class DatasetUpdaterConfiguration {

  @ParametersDelegate @Valid @NotNull public DbConfiguration db = new DbConfiguration();

  @Parameter(names = "--dataset-key")
  public String key;

  @Parameter(names = "--dataset-key-path")
  public String keyFilePath;

  @Override
  public String toString() {
    return new StringJoiner(", ", DatasetUpdaterConfiguration.class.getSimpleName() + "[", "]")
        .add("db=" + db)
        .add("key='" + key + "'")
        .add("keyFilePath='" + keyFilePath + "'")
        .toString();
  }
}
