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

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Identifier;
import org.gbif.doi.service.DoiService;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.registry.cli.common.CommonBuilder;
import org.gbif.registry.cli.util.RegistryCliUtils;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.gbif.registry.cli.util.RegistryCliUtils.getFileData;
import static org.gbif.registry.cli.util.RegistryCliUtils.prepareConnection;

@SuppressWarnings("ConstantConditions")
public class DoiSynchronizerCommandIT {

  private static final DOI DOI1 = new DOI("10.21373/gbif.1584932725458");
  private static final URI DOI_TARGET =
      URI.create("https://registry.gbif-dev.org/dataset/665de833-60e1-44ab-9ac1-8ef33ed8d9dd");

  private static DoiSynchronizerCommand command;
  private static DoiSynchronizerConfiguration doiSynchronizerConfig;
  private static String metadataXml;

  @BeforeClass
  public static void beforeClass() throws Exception {
    byte[] bytes =
        Files.readAllBytes(
            Paths.get(
                ClassLoader.getSystemClassLoader()
                    .getResource("doisynchronizer/minimal-metadata.xml")
                    .getFile()));

    metadataXml = new String(bytes);

    doiSynchronizerConfig =
        RegistryCliUtils.loadConfig(
            "doisynchronizer/doi-synchronizer.yaml", DoiSynchronizerConfiguration.class);
    command = new DoiSynchronizerCommand(doiSynchronizerConfig);

    prepareDataCiteData();
  }

  // check & prepare required DataCite data
  private static void prepareDataCiteData() throws Exception {
    DoiService doiService =
        CommonBuilder.createRestJsonApiDataCiteService(doiSynchronizerConfig.datacite);

    DoiData doiData = doiService.resolve(DOI1);
    if (doiData.getStatus() != DoiStatus.REGISTERED) {
      doiService.register(DOI1, DOI_TARGET, prepareMetadata(DOI1));
    } else {
      doiService.update(DOI1, prepareMetadata(DOI1));
    }
  }

  @Before
  public void prepareDatabase() throws Exception {
    Connection con = prepareConnection(doiSynchronizerConfig.registry);
    String sql = getFileData("doisynchronizer/prepare_dataset.sql");

    PreparedStatement stmt = con.prepareStatement(sql);
    stmt.executeUpdate();
    con.close();
  }

  @After
  public void after() throws Exception {
    Connection con = prepareConnection(doiSynchronizerConfig.registry);
    String sql = getFileData("doisynchronizer/clean_dataset.sql");

    PreparedStatement stmt = con.prepareStatement(sql);
    stmt.executeUpdate();
    con.close();
  }

  @Test
  public void name() {
    doiSynchronizerConfig.fixDOI = true;
    doiSynchronizerConfig.doi = DOI1.toString();

    command.doRun();
  }

  private static DataCiteMetadata prepareMetadata(DOI doi) throws Exception {
    DataCiteMetadata metadata = DataCiteValidator.fromXml(metadataXml);

    metadata.setIdentifier(
        Identifier.builder().withIdentifierType("DOI").withValue(doi.toString()).build());

    return metadata;
  }
}
