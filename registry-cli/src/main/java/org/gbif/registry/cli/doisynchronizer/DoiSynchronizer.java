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
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.DoiService;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.doi.util.MetadataUtils;
import org.gbif.registry.cli.common.SingleColumnFileReader;
import org.gbif.registry.cli.common.spring.SpringContextBuilder;
import org.gbif.registry.cli.doisynchronizer.diagnostic.DoiDiagnosticPrinter;
import org.gbif.registry.cli.doisynchronizer.diagnostic.GbifDOIDiagnosticResult;
import org.gbif.registry.cli.doisynchronizer.diagnostic.GbifDatasetDOIDiagnosticResult;
import org.gbif.registry.cli.doisynchronizer.diagnostic.GbifDownloadDOIDiagnosticResult;
import org.gbif.registry.doi.handler.DataCiteDoiHandlerStrategy;
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

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * This service allows to print a report of DOI and/or try to fix them by synchronizing with
 * Datacite. This service is mainly design to be run manually and uses System.out.
 */
public class DoiSynchronizer {

  private static final Logger LOG = LoggerFactory.getLogger(DoiSynchronizer.class);

  private ApplicationContext context;
  private final DoiSynchronizerConfiguration config;
  private final DoiMapper doiMapper;
  private final DataCiteDoiHandlerStrategy dataCiteDoiHandlerStrategy;
  private final DatasetMapper datasetMapper;
  private final OccurrenceDownloadMapper downloadMapper;
  private final DoiService dataCiteService;
  private final UserMapper userMapper;

  private final DoiDiagnosticPrinter diagnosticPrinter = new DoiDiagnosticPrinter(System.out);

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
    doiMapper = context.getBean(DoiMapper.class);
    dataCiteDoiHandlerStrategy = context.getBean(DataCiteDoiHandlerStrategy.class);
    datasetMapper = context.getBean(DatasetMapper.class);
    downloadMapper = context.getBean(OccurrenceDownloadMapper.class);
    userMapper = context.getBean(UserMapper.class);
    dataCiteService = context.getBean(DoiService.class);
  }

  /** Runs the actual service */
  public void doRun() {
    if (validateDOIParameters()) {
      // Single DOI
      if (StringUtils.isNotBlank(config.doi)) {
        handleDOI(config.doi);
      } // DOI list
      else if (StringUtils.isNotBlank(config.doiList)) {
        SingleColumnFileReader.readFile(config.doiList, SingleColumnFileReader::toDoi)
            .forEach(this::handleDOI);
      } else if (config.listFailedDOI) {
        printFailedDOI();
      }
    }
  }

  /**
   * Check the current status of the DOI related configurations from the instance of {@link
   * DoiSynchronizerConfiguration}. The method is intended to be used on command line and will print
   * messages using System.out.
   */
  private boolean validateDOIParameters() {
    if (config.listFailedDOI
        && (StringUtils.isNotBlank(config.doi)
            || StringUtils.isNotBlank(config.doiList)
            || config.export
            || config.fixDOI)) {
      System.out.println(" --list-failed-doi must be used alone");
      return false;
    }

    if (StringUtils.isNotBlank(config.doi) && StringUtils.isNotBlank(config.doiList)) {
      System.out.println(" --doi and --doi-list can not be used at the same time");
      return false;
    }

    if (config.export && StringUtils.isNotBlank(config.doiList)) {
      System.out.println(" --export can not be used with --doi-list");
      return false;
    }

    if (StringUtils.isNotBlank(config.doi)) {
      if (!DOI.isParsable(config.doi)) {
        System.out.println(config.doi + " is not a valid DOI");
        return false;
      }
    } else if (StringUtils.isNotBlank(config.doiList)) {
      if (!new File(config.doiList).exists()) {
        System.out.println("DOI list can not be found: " + config.doiList);
        return false;
      }
    }
    return true;
  }

  /** Handle a single DOIm provided as String */
  private void handleDOI(String doiAsString) {
    try {
      handleDOI(new DOI(doiAsString));
    } catch (IllegalArgumentException iaEx) {
      System.out.println(config.doi + " is not a valid DOI");
    }
  }

  /** Handle a single DOI */
  private void handleDOI(DOI doi) {
    if (!config.skipDiagnostic) {
      reportDOIStatus(doi);
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
      diagnosticPrinter.printDOIFixAttemptReport(doi, tryFixDOI(doi));
    }
  }

  /** Report the current status of a DOI */
  private void reportDOIStatus(DOI doi) {
    GbifDOIDiagnosticResult doiDiagnostic = generateGbifDOIDiagnostic(doi);

    if (doiDiagnostic != null) {
      diagnosticPrinter.printReport(doiDiagnostic);
    } else {
      System.out.println("No report can be generated. Nothing found for DOI " + doi);
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

    // ensure we only have 1 dataset linked to this DOI
    if (datasetsFromDOI.size() != 1) {
      return false;
    }

    // get the Dataset from the right source
    Dataset dataset = datasetsFromDOI.get(0);

    DOI datasetDoi = dataset.getDoi();
    if (doi.equals(datasetDoi)) {
      dataCiteDoiHandlerStrategy.datasetChanged(dataset, null);
      return true;
    }
    // DOI changed

    // The dataset DOI is not issued by GBIF
    if (!dataCiteDoiHandlerStrategy.isUsingMyPrefix(datasetDoi)) {
      boolean doiIsInAlternateIdentifiers = isIdentifierDOIFound(doi, dataset);
      boolean datasetDoiIsInAlternateIdentifiers = isIdentifierDOIFound(datasetDoi, dataset);
      // check we are in a known state which means:
      // - The current dataset DOI is not in alternative identifiers but the previous GBIF DOI is
      // the logic is applied by the registry
      if (doiIsInAlternateIdentifiers && !datasetDoiIsInAlternateIdentifiers) {
        dataCiteDoiHandlerStrategy.datasetChanged(dataset, doi);
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

  /** Checks if a DOI can be found in the list of dataset identifiers. */
  private static boolean isIdentifierDOIFound(DOI doi, Identifiable identifiable) {
    return identifiable.getIdentifiers().stream()
        .anyMatch(
            identifier ->
                IdentifierType.DOI == identifier.getType()
                    && identifier.getIdentifier().equals(doi.toString()));
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

    dataCiteDoiHandlerStrategy.downloadChanged(download, null, user);

    return true;
  }

  /**
   * Compare the metadata linked to a DOI between what we have in the database and what is stored at
   * Datacite.
   */
  private boolean compareMetadataWithDatacite(DOI doi) {
    String registryDoiMetadataStr = doiMapper.getMetadata(doi);
    String dataCiteDoiMetadataStr = null;
    try {
      dataCiteDoiMetadataStr = dataCiteService.getMetadata(doi);
    } catch (DoiException e) {
      LOG.error("Can't compare DOI metadata", e);
    }

    try {
      DataCiteMetadata registryDoiMetadata =
          registryDoiMetadataStr != null ? DataCiteValidator.fromXml(registryDoiMetadataStr) : null;
      DataCiteMetadata dataCiteDoiMetadata =
          dataCiteDoiMetadataStr != null ? DataCiteValidator.fromXml(dataCiteDoiMetadataStr) : null;

      // Use this method unless it's always false because the DataCite store DOI in uppercase a
      // nd the registry store in lower case
      return MetadataUtils.equal(registryDoiMetadata, dataCiteDoiMetadata);
    } catch (JAXBException e) {
      LOG.error("Invalid metadata", e);
    }

    return false;
  }

  /** Get the list of failed DOI for a DoiType. */
  private void printFailedDOI() {
    // get all the DOI with the FAILED status. Note that they are all GBIF assigned DOI.
    doiMapper
        .list(DoiStatus.FAILED, null, null)
        .forEach(
            map ->
                System.out.println(
                    MessageFormat.format("{0} ({1})", map.get("doi"), map.get("type"))));
  }

  /** Check the status of a DOI between GBIF and Datacite. */
  private GbifDOIDiagnosticResult generateGbifDOIDiagnostic(DOI doi) {
    GbifDOIDiagnosticResult doiGbifDataciteDiagnostic = null;

    DoiType doiType = doiMapper.getType(doi);
    if (doiType != null) {
      if (doiType == DoiType.DATASET) {
        doiGbifDataciteDiagnostic = createGbifDOIDatasetDiagnostic(doi);
      } else if (doiType == DoiType.DOWNLOAD) {
        doiGbifDataciteDiagnostic = createGbifDOIDownloadDiagnostic(doi);
      }
    }

    if (doiGbifDataciteDiagnostic == null) {
      return null;
    }

    DoiData doiData = doiMapper.get(doi);
    doiGbifDataciteDiagnostic.setDoiData(doiData);

    try {
      doiGbifDataciteDiagnostic.setDoiExistsAtDatacite(dataCiteService.exists(doi));
    } catch (DoiException e) {
      LOG.warn("Can not check existence of DOI " + doi.getDoiName(), e);
    }

    if (doiGbifDataciteDiagnostic.isDoiExistsAtDatacite()) {
      doiGbifDataciteDiagnostic.setMetadataEquals(compareMetadataWithDatacite(doi));

      try {
        DoiData doiStatus = dataCiteService.resolve(doi);
        doiGbifDataciteDiagnostic.setDataciteDoiStatus(doiStatus.getStatus());
        doiGbifDataciteDiagnostic.setDataciteTarget(doiStatus.getTarget());
      } catch (DoiException e) {
        LOG.error("Failed to resolve DOI {}", doi);
      }
    }
    return doiGbifDataciteDiagnostic;
  }

  private GbifDOIDiagnosticResult createGbifDOIDatasetDiagnostic(DOI doi) {
    GbifDatasetDOIDiagnosticResult datasetDiagnosticResult =
        new GbifDatasetDOIDiagnosticResult(doi);

    // Try to load the Dataset from its DOI and alternate identifier
    datasetDiagnosticResult.appendRelatedDataset(datasetMapper.listByDOI(doi.getDoiName(), null));

    if (datasetDiagnosticResult.isLinkedToASingleDataset()) {
      datasetDiagnosticResult.setDoiIsInAlternateIdentifiers(
          isIdentifierDOIFound(doi, datasetDiagnosticResult.getRelatedDataset()));
    }

    return datasetDiagnosticResult;
  }

  private GbifDOIDiagnosticResult createGbifDOIDownloadDiagnostic(DOI doi) {
    GbifDownloadDOIDiagnosticResult downloadDiagnosticResult =
        new GbifDownloadDOIDiagnosticResult(doi);

    Download download = downloadMapper.getByDOI(doi);
    downloadDiagnosticResult.setDownload(download);

    return downloadDiagnosticResult;
  }

  public ApplicationContext getContext() {
    return context;
  }
}
