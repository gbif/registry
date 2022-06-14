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
package org.gbif.registry.cli.datasetindex.indexupdater;

import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import org.kohsuke.MetaInfServices;

import com.google.common.util.concurrent.Service;
/** This command updates either a single dataset or a list of datasets in the ES index. */
@MetaInfServices(Command.class)
public class DatasetIndexUpdaterCommand extends ServiceCommand {

  private final DatasetIndexUpdaterConfiguration config = new DatasetIndexUpdaterConfiguration();

  public DatasetIndexUpdaterCommand() {
    super("dataset-index-updater");
  }

  @Override
  protected Service getService() {
    return new DatasetIndexUpdaterService(config);
  }

  @Override
  protected Object getConfigurationObject() {
    return config;
  }
}
