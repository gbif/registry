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
package org.gbif.registry.cli.doisynchronizer.diagnostic;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.occurrence.Download;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.DoiService;
import org.gbif.doi.util.Difference;
import org.gbif.doi.util.MetadataUtils;
import org.gbif.registry.cli.doisynchronizer.DoiSynchronizerConfiguration;
import org.gbif.registry.doi.util.RegistryDoiUtils;
import org.gbif.registry.domain.doi.DoiType;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.DoiMapper;
import org.gbif.registry.persistence.mapper.OccurrenceDownloadMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoiDiagnostician {

  private static final Logger LOG = LoggerFactory.getLogger(DoiDiagnostician.class);

  private final DoiDiagnosticPrinter diagnosticPrinter;
  private final DoiMapper doiMapper;
  private final DoiService dataCiteService;
  private final DatasetMapper datasetMapper;
  private final OccurrenceDownloadMapper downloadMapper;

  public DoiDiagnostician(
      DoiMapper doiMapper,
      DoiService dataCiteService,
      DatasetMapper datasetMapper,
      OccurrenceDownloadMapper downloadMapper,
      DoiSynchronizerConfiguration config) {
    this.doiMapper = doiMapper;
    this.dataCiteService = dataCiteService;
    this.datasetMapper = datasetMapper;
    this.downloadMapper = downloadMapper;
    this.diagnosticPrinter = new DoiDiagnosticPrinter(System.out, config);
  }

  /** Report the current status of a DOI */
  public void reportDOIStatus(DOI doi) {
    GbifDOIDiagnosticResult doiDiagnostic = generateGbifDOIDiagnostic(doi);

    if (doiDiagnostic != null) {
      diagnosticPrinter.printReport(doiDiagnostic);
    } else {
      System.out.println("No report can be generated. Nothing found for DOI " + doi);
    }
  }

  /** Check the status of a DOI between GBIF and Datacite. */
  public GbifDOIDiagnosticResult generateGbifDOIDiagnostic(DOI doi) {
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
      boolean metadataEquals = false;
      String registryDoiMetadataXml = doiMapper.getMetadata(doi);
      String dataCiteDoiMetadataXml;
      try {
        dataCiteDoiMetadataXml = dataCiteService.getMetadata(doi);
        metadataEquals =
            MetadataUtils.metadataEquals(registryDoiMetadataXml, dataCiteDoiMetadataXml);

        Difference difference = MetadataUtils.metadataDifference(registryDoiMetadataXml, dataCiteDoiMetadataXml);
        doiGbifDataciteDiagnostic.setDifference(difference.toString());
      } catch (DoiException e) {
        LOG.error("Can't compare DOI metadata", e);
      }

      doiGbifDataciteDiagnostic.setMetadataEquals(metadataEquals);

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

  public GbifDOIDiagnosticResult createGbifDOIDatasetDiagnostic(DOI doi) {
    GbifDatasetDOIDiagnosticResult datasetDiagnosticResult =
        new GbifDatasetDOIDiagnosticResult(doi);

    // Try to load the Dataset from its DOI and alternate identifier
    datasetDiagnosticResult.appendRelatedDataset(datasetMapper.listByDOI(doi.getDoiName(), null));

    if (datasetDiagnosticResult.isLinkedToASingleDataset()) {
      datasetDiagnosticResult.setDoiIsInAlternateIdentifiers(
          RegistryDoiUtils.isIdentifierDOIFound(doi, datasetDiagnosticResult.getRelatedDataset()));
    }

    return datasetDiagnosticResult;
  }

  public GbifDOIDiagnosticResult createGbifDOIDownloadDiagnostic(DOI doi) {
    GbifDownloadDOIDiagnosticResult downloadDiagnosticResult =
        new GbifDownloadDOIDiagnosticResult(doi);

    Download download = downloadMapper.getByDOI(doi);
    downloadDiagnosticResult.setDownload(download);

    return downloadDiagnosticResult;
  }
}
