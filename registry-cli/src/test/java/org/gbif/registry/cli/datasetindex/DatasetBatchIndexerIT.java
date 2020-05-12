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
package org.gbif.registry.cli.datasetindex;

import org.gbif.registry.cli.common.DbConfiguration;
import org.gbif.registry.search.test.EsManageServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.io.InputStreamResource;

import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.SingleInstancePostgresExtension;

public class DatasetBatchIndexerIT {

  @RegisterExtension
  public static SingleInstancePostgresExtension database =
      EmbeddedPostgresExtension.singleInstance();

  private static EsManageServer esManageServer;

  DatasetBatchIndexerConfiguration getConfiguration() {
    ElasticsearchConfig elasticsearchConfigDataset = new ElasticsearchConfig();
    elasticsearchConfigDataset.setHosts(esManageServer.getServerAddress());
    elasticsearchConfigDataset.setIndex("dataset");

    DatasetBatchIndexerConfiguration configuration = new DatasetBatchIndexerConfiguration();
    configuration.setDatasetEs(elasticsearchConfigDataset);
    configuration.setOccurrenceEs(elasticsearchConfigDataset);
    configuration.setApiRootUrl("http://api.gbif-dev.org/v1/");

    DbConfiguration dbConfiguration = new DbConfiguration();
    dbConfiguration.serverName = "localhost:" + database.getEmbeddedPostgres().getPort();
    dbConfiguration.databaseName = "postgres";
    dbConfiguration.user = "";
    dbConfiguration.password = "";

    configuration.setClbDb(dbConfiguration);
    return configuration;
  }

  @BeforeAll
  public static void init() throws Exception {
    esManageServer =
        new EsManageServer(
            new InputStreamResource(
                DatasetBatchIndexerIT.class
                    .getClassLoader()
                    .getResourceAsStream("dataset-es-mapping.json")),
            "dataset",
            "dataset");
    esManageServer.start();
  }

  @Test
  public void fullIndexTests() {
    DatasetBatchIndexerCommand datasetBatchIndexerCommand =
        new DatasetBatchIndexerCommand(getConfiguration());
    datasetBatchIndexerCommand.doRun();
  }

  @AfterAll
  public static void finish() throws Exception {
    if (esManageServer != null) {
      esManageServer.destroy();
    }
  }
}
