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
package org.gbif.registry.doi;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;

@Service
public class DownloadDoiDataCiteHandlingServiceImpl implements DownloadDoiDataCiteHandlingService {

  // DOI logging marker
  private static final Logger LOG =
      LoggerFactory.getLogger(DownloadDoiDataCiteHandlingServiceImpl.class);
  private static final Marker DOI_SMTP = MarkerFactory.getMarker("DOI_SMTP");

  private final DoiIssuingService doiIssuingService;

  private final DoiMessageManagingService doiMessageManagingService;
  private final DataCiteMetadataBuilderService metadataBuilderService;

  public DownloadDoiDataCiteHandlingServiceImpl(
    DoiIssuingService doiIssuingService, DoiMessageManagingService doiMessageManagingService,
      DataCiteMetadataBuilderService metadataBuilderService) {
    this.doiIssuingService = doiIssuingService;
    this.doiMessageManagingService = doiMessageManagingService;
    this.metadataBuilderService = metadataBuilderService;
  }

  /**
   * Updates the download DOI according to the download status. If the download succeeded its DOI is
   * registered; if the download status is one the FAILED_STATES the DOI is removed, otherwise does
   * nothing.
   */
  @Override
  public void downloadChanged(Download download, Download previousDownload, GbifUser user) {
    Preconditions.checkNotNull(download, "download can not be null");

    if (download.isAvailable()
        && (previousDownload == null
            || (previousDownload.getStatus() != Download.Status.SUCCEEDED
                && previousDownload.getStatus() != Download.Status.FILE_ERASED))) {
      try {
        if (download.getStatus().equals(Download.Status.SUCCEEDED) && download.getDoi() == null) {
          download.setDoi(doiIssuingService.newDownloadDOI());
        }
        doiMessageManagingService.registerDownload(
            download.getDoi(),
            metadataBuilderService.buildMetadata(download, user),
            download.getKey(),
            download.getRequest().getType());
      } catch (Exception error) {
        LOG.error(
            DOI_SMTP,
            "Invalid metadata for download {} with doi {} ",
            download.getKey(),
            download.getDoi(),
            error);
      }
    }
  }
}
