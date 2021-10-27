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

import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;

import org.kohsuke.MetaInfServices;

/**
 * This command update either a single dataset or a list of datasets by reinterpreting their
 * preferred metadata document stored in the registry.
 */
@MetaInfServices(Command.class)
public class DatasetBatchIndexerCommand extends BaseCommand {

  private final DatasetBatchIndexerConfiguration config;
  private DatasetBatchIndexer batchIndexer;

  public DatasetBatchIndexerCommand() {
    super("dataset-batch-indexer");
    config = new DatasetBatchIndexerConfiguration();
  }

  // constructor for tests
  public DatasetBatchIndexerCommand(DatasetBatchIndexerConfiguration config) {
    super("dataset-batch-indexer");
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
    batchIndexer =
        SpringContextBuilder.applicationContext(config).getBean(DatasetBatchIndexer.class);
    batchIndexer.run(config);
  }

  public DatasetBatchIndexer getDatasetBatchIndexer() {
    return batchIndexer;
  }
}
