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
import org.gbif.api.model.common.DoiStatus;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.service.DoiService;
import org.gbif.registry.cli.common.SingleColumnFileReader;
import org.gbif.registry.cli.common.spring.SpringContextBuilder;
import org.gbif.registry.cli.doisynchronizer.diagnostic.DoiDiagnostician;
import org.gbif.registry.doi.DatasetDoiDataCiteHandlingService;
import org.gbif.registry.doi.DoiIssuingService;
import org.gbif.registry.doi.DownloadDoiDataCiteHandlingService;
import org.gbif.registry.domain.doi.DoiType;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.DoiMapper;
import org.gbif.registry.persistence.mapper.OccurrenceDownloadMapper;
import org.gbif.registry.persistence.mapper.UserMapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import static org.gbif.registry.doi.util.RegistryDoiUtils.isIdentifierDOIFound;

/**
 * This service allows to print a report of DOI and/or try to fix them by synchronizing with
 * Datacite. This service is mainly design to be run manually and uses System.out.
 */
public class DoiSynchronizer {

  private static final Logger LOG = LoggerFactory.getLogger(DoiSynchronizer.class);

  private ApplicationContext context;
  private final DoiSynchronizerConfiguration config;
  private final DoiMapper doiMapper;
  private final DatasetDoiDataCiteHandlingService datasetDoiDataCiteHandlingService;
  private final DownloadDoiDataCiteHandlingService downloadDoiDataCiteHandlingService;
  private final DoiIssuingService doiIssuingService;
  private final DatasetMapper datasetMapper;
  private final OccurrenceDownloadMapper downloadMapper;
  private final UserMapper userMapper;
  private final DoiDiagnostician diagnostician;

  public DoiSynchronizer(DoiSynchronizerConfiguration config) {
    this(
        config,
        SpringContextBuilder.create()
            .withDoiSynchronizerConfiguration(config)
            .withScanPackages("org.gbif.registry.doi")
            .build());
  }

  public DoiSynchronizer(DoiSynchronizerConfiguration config, ApplicationContext context) {
    this.config = config;
    this.context = context;
    this.doiMapper = context.getBean(DoiMapper.class);
    this.doiIssuingService = context.getBean(DoiIssuingService.class);
    this.datasetDoiDataCiteHandlingService =
        context.getBean(DatasetDoiDataCiteHandlingService.class);
    this.downloadDoiDataCiteHandlingService =
        context.getBean(DownloadDoiDataCiteHandlingService.class);
    this.datasetMapper = context.getBean(DatasetMapper.class);
    this.downloadMapper = context.getBean(OccurrenceDownloadMapper.class);
    this.userMapper = context.getBean(UserMapper.class);
    this.diagnostician =
        new DoiDiagnostician(
            doiMapper, context.getBean(DoiService.class), datasetMapper, downloadMapper);
  }

  /** Handle a single DOI provided as String */
  public void handleDOI() {
    try {
      handleDOI(new DOI(config.doi));
    } catch (IllegalArgumentException iaEx) {
      System.out.println(config.doi + " is not a valid DOI");
    }
  }

  /** Handle a list of DOIs provided as a file name */
  public void handleListDOI() {
    SingleColumnFileReader.readFile(config.doiList, SingleColumnFileReader::toDoi)
        .forEach(this::handleDOI);
  }

  /** Handle a single DOI */
  private void handleDOI(DOI doi) {
    if (!config.skipDiagnostic) {
      diagnostician.reportDOIStatus(doi);
    }

    if (config.export) {
      String registryDoiMetadata = doiMapper.getMetadata(doi);
      if (StringUtils.isNotEmpty(registryDoiMetadata)) {
        File exportTo = new File(doi.getDoiName().replace("/", "_") + "_export.xml");
        try {
          FileUtils.writeStringToFile(exportTo, registryDoiMetadata, StandardCharsets.UTF_8);
          System.out.println("Exported file saved in " + exportTo.getAbsolutePath());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    if (config.fixDOI) {
      boolean result = tryFixDOI(doi);
      System.out.println(
          "Attempt to fix DOI " + doi.getDoiName() + " : " + (result ? "success" : "failed"));
    }
  }

  /** Try to fix a DOI if possible */
  private boolean tryFixDOI(DOI doi) {
    DoiType doiType = doiMapper.getType(doi);
    if (doiType == null) {
      return false;
    }

    if (doiType == DoiType.DATASET) {
      return reapplyDatasetDOIStrategy(doi);
    } else if (doiType == DoiType.DOWNLOAD) {
      return reapplyDownloadDOIStrategy(doi);
    }

    return false;
  }

  /**
   * Re-apply the DataCite DOI handling strategy if possible.
   *
   * @return DataCite DOI handling strategy applied?
   */
  private boolean reapplyDatasetDOIStrategy(DOI doi) {
    Objects.requireNonNull(doi, "DOI can't be null");

    List<Dataset> datasetsFromDOI = datasetMapper.listByDOI(doi.getDoiName(), null);

    // check that we have something to work on
    if (datasetsFromDOI.isEmpty()) {
      return false;
    }

    Dataset dataset = datasetsFromDOI.get(0);

    // cound non-deleted datasets and remember the last one
    int countNonDeleted = 0;
    for (Dataset d : datasetsFromDOI) {
      if (d.getDeleted() != null) {
        countNonDeleted++;
        dataset = d;
      }
    }

    // ensure we have only one non-deleted dataset
    if (countNonDeleted != 1) {
      return false;
    }

    DOI datasetDoi = dataset.getDoi();
    if (doi.equals(datasetDoi)) {
      datasetDoiDataCiteHandlingService.datasetChanged(dataset, null);
      return true;
    }
    // DOI changed

    // The dataset DOI is not issued by GBIF
    if (!doiIssuingService.isGbif(datasetDoi)) {
      boolean doiIsInAlternateIdentifiers = isIdentifierDOIFound(doi, dataset);
      boolean datasetDoiIsInAlternateIdentifiers = isIdentifierDOIFound(datasetDoi, dataset);
      // check we are in a known state which means:
      // - The current dataset DOI is not in alternative identifiers but the previous GBIF DOI is
      // the logic is applied by the registry
      if (doiIsInAlternateIdentifiers && !datasetDoiIsInAlternateIdentifiers) {
        datasetDoiDataCiteHandlingService.datasetChanged(dataset, doi);
        return true;
      } else {
        LOG.error(
            "Can not handle cases where the DOI changed but the list of alternate identifiers is not updated");
      }
    } else {
      LOG.error("Can not handle cases where the DOI changed to a GBIF DOI");
    }

    return false;
  }

  /**
   * Re-apply the Download DOI strategy from dataCiteDoiHandlerStrategy.
   *
   * @return success or not
   */
  private boolean reapplyDownloadDOIStrategy(DOI doi) {
    Objects.requireNonNull(doi, "DOI can't be null");

    Download download = downloadMapper.getByDOI(doi);
    if (download == null) {
      return false;
    }

    // only handle download with status SUCCEEDED or FILE_ERASED
    if (Download.Status.SUCCEEDED != download.getStatus()
        && Download.Status.FILE_ERASED != download.getStatus()) {
      LOG.error("Download with DOI {} status is {}", doi, download.getStatus());
      return false;
    }

    // retrieve User
    String creatorName = download.getRequest().getCreator();
    GbifUser user = userMapper.get("occdownload.gbif.org");

    if (user == null) {
      LOG.error("No user with creator name {} can be found", creatorName);
      return false;
    }

    downloadDoiDataCiteHandlingService.downloadChanged(download, null, user);

    return true;
  }

  /** Get the list of failed DOI for a DoiType. */
  public void printFailedDOI() {
    // get all the DOI with the FAILED status. Note that they are all GBIF assigned DOI.
    doiMapper
        .list(DoiStatus.FAILED, null, null)
        .forEach(
            map ->
                System.out.println(
                    MessageFormat.format("{0} ({1})", map.get("doi"), map.get("type"))));
  }

  public ApplicationContext getContext() {
    return context;
  }
}
