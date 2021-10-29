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

import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;
import org.gbif.registry.cli.common.SingleColumnFileReader;

import java.util.List;
import java.util.UUID;

import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This command update either a single dataset or a list of datasets by reinterpreting their
 * preferred metadata document stored in the registry.
 */
@MetaInfServices(Command.class)
public class DatasetUpdaterCommand extends BaseCommand {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetUpdaterCommand.class);

  private final DatasetUpdaterConfiguration config;
  private DatasetUpdater updater;

  public DatasetUpdaterCommand() {
    super("dataset-updater");
    config = new DatasetUpdaterConfiguration();
  }

  // constructor for tests
  public DatasetUpdaterCommand(DatasetUpdaterConfiguration config) {
    super("dataset-updater");
    this.config = config;
  }

  @Override
  protected Object getConfigurationObject() {
    return config;
  }

  /**
   * Updates single dataset from dataset key parameter if detected. </br> Otherwise, updates list of
   * datasets from keyFilePath parameter if detected. </br> In case neither parameters are detected,
   * method exists.
   */
  @Override
  protected void doRun() {
    if (config.key == null && config.keyFilePath == null) {
      LOG.error("Both key and keyFilePath are null - one of them has to be set. Exiting.");
      return;
    }

    updater = DatasetUpdater.build(config);

    if (config.key != null) {
      updater.update(UUID.fromString(config.key));
      LOG.info("{} out of 1 datasets were updated", updater.getUpdateCounter());
    } else {
      List<UUID> keys =
          SingleColumnFileReader.readFile(config.keyFilePath, SingleColumnFileReader::toUuid);
      updater.update(keys);
      LOG.info("{} out of {} datasets were updated", updater.getUpdateCounter(), keys.size());
    }
  }

  public DatasetUpdater getDatasetUpdater() {
    return updater;
  }
}
