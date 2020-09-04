package org.gbif.registry.doi;

import com.google.common.base.Preconditions;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

@Service
public class DownloadDoiDataCiteHandlingServiceImpl implements DownloadDoiDataCiteHandlingService {

  // DOI logging marker
  private static final Logger LOG = LoggerFactory.getLogger(DownloadDoiDataCiteHandlingServiceImpl.class);
  private static final Marker DOI_SMTP = MarkerFactory.getMarker("DOI_SMTP");

  private static final EnumSet<Download.Status> FAILED_STATES =
      EnumSet.of(Download.Status.KILLED, Download.Status.CANCELLED, Download.Status.FAILED);

  private final DoiMessageManagingService doiMessageManagingService;
  private final DataCiteMetadataBuilderService metadataBuilderService;

  public DownloadDoiDataCiteHandlingServiceImpl(
      DoiMessageManagingService doiMessageManagingService,
      DataCiteMetadataBuilderService metadataBuilderService) {
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
        doiMessageManagingService.registerDownload(
            download.getDoi(), metadataBuilderService.buildMetadata(download, user), download.getKey());
      } catch (Exception error) {
        LOG.error(
            DOI_SMTP,
            "Invalid metadata for download {} with doi {} ",
            download.getKey(),
            download.getDoi(),
            error);
      }
    } else if (FAILED_STATES.contains(download.getStatus())) {
      doiMessageManagingService.delete(download.getDoi());
    }
  }
}
