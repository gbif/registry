package org.gbif.registry.doi.handler;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.User;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.RelatedIdentifierType;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.occurrence.query.TitleLookup;
import org.gbif.registry.doi.DataCiteConverter;
import org.gbif.registry.doi.generator.DoiGenerator;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;


/**
 *
 */
public class GbifDataCiteDOIHandlerStrategy implements DataCiteDOIHandlerStrategy {

  //DOI logging marker
  private static final Logger LOG = LoggerFactory.getLogger(GbifDataCiteDOIHandlerStrategy.class);
  private static Marker DOI_SMTP = MarkerFactory.getMarker("DOI_SMTP");

  //Page size to iterate over dataset usages
  private static final int USAGES_PAGE_SIZE = 400;

  private final EnumSet<Download.Status> FAILED_STATES = EnumSet.of(Download.Status.KILLED, Download.Status.CANCELLED,
          Download.Status.FAILED);

  private final DoiGenerator doiGenerator;
  private final OrganizationService organizationService;
  private final OccurrenceDownloadService occurrenceDownloadService;
  private final TitleLookup titleLookup;

  @Inject
  public GbifDataCiteDOIHandlerStrategy(DoiGenerator doiGenerator, OrganizationService organizationService,
                                        OccurrenceDownloadService occurrenceDownloadService,
                                        TitleLookup titleLookup) {
    this.doiGenerator = doiGenerator;
    this.organizationService = organizationService;
    this.occurrenceDownloadService = occurrenceDownloadService;
    this.titleLookup = titleLookup;
  }

  @Override
  public DataCiteMetadata buildMetadata(Download download, User user) {
    List<DatasetOccurrenceDownloadUsage> response = null;
    List<DatasetOccurrenceDownloadUsage> usages = Lists.newArrayList();
    PagingRequest pagingRequest = new PagingRequest(0, USAGES_PAGE_SIZE);

    while (response == null || !response.isEmpty()) {
      response = occurrenceDownloadService.listDatasetUsages(download.getKey(), pagingRequest).getResults();
      usages.addAll(response);
      pagingRequest.nextPage();
    }

    return DataCiteConverter.convert(download, user, usages, titleLookup);
  }

  @Override
  public DataCiteMetadata buildMetadata(Dataset dataset) {
    return buildMetadata(dataset, null, null);
  }

  @Override
  public DataCiteMetadata buildMetadata(Dataset dataset, @Nullable DOI related, @Nullable RelationType relationType) {
    Organization publisher = organizationService.get(dataset.getPublishingOrganizationKey());
    DataCiteMetadata m = DataCiteConverter.convert(dataset, publisher);
    // add previous relationship
    if (related != null) {
      m.getRelatedIdentifiers().getRelatedIdentifier()
              .add(DataCiteMetadata.RelatedIdentifiers.RelatedIdentifier.builder()
                              .withRelationType(relationType)
                              .withValue(related.getDoiName())
                              .withRelatedIdentifierType(RelatedIdentifierType.DOI)
                              .build()
              );
    }
    return m;
  }

  @Override
  public void datasetChanged(Dataset dataset, @Nullable DOI previousDoi) {
    // if the old doi was a GBIF one and the new one is different, update its metadata with a version relationship
    if (previousDoi != null && doiGenerator.isGbif(previousDoi) && !dataset.getDoi().equals(previousDoi)) {
      scheduleDatasetRegistration(previousDoi, buildMetadata(dataset, dataset.getDoi(), RelationType.IS_PREVIOUS_VERSION_OF),
              dataset.getKey());
    }
    // if the current doi was a GBIF DOI finally schedule a metadata update in datacite
    if (doiGenerator.isGbif(dataset.getDoi())) {
      // if DOIs changed establish relationship
      DataCiteMetadata metadata;
      // to get the latest timestamps we need to read a new copy of the dataset
      if (previousDoi == null || dataset.getDoi().equals(previousDoi)) {
        metadata = buildMetadata(dataset);
      } else {
        metadata = buildMetadata(dataset, previousDoi, RelationType.IS_NEW_VERSION_OF);
      }
      scheduleDatasetRegistration(dataset.getDoi(), metadata, dataset.getKey());
    }
  }

  /**
   * Updates the download DOI according to the download status.
   * If the download succeeded its DOI is registered; if the download status is one the FAILED_STATES
   * the DOI is removed, otherwise doesn't nothing.
   */
  @Override
  public void downloadChanged(Download download, Download previousDownload, User user) {
    if (download.isAvailable() && previousDownload.getStatus() != Download.Status.SUCCEEDED) {
      try {
        doiGenerator.registerDownload(download.getDoi(), buildMetadata(download, user), download.getKey());
      } catch (Exception error) {
        LOG.error(DOI_SMTP, "Invalid metadata for download {} with doi {} ", download.getKey(), download.getDoi(), error);
      }

    } else if (FAILED_STATES.contains(download.getStatus())) {
      doiGenerator.delete(download.getDoi());
    }
  }

  public void scheduleDatasetRegistration(DOI doi, DataCiteMetadata metadata, UUID datasetKey) {
    try {
      doiGenerator.registerDataset(doi, metadata, datasetKey);
    } catch (InvalidMetadataException e) {
      LOG.error(DOI_SMTP, "Failed to schedule DOI update for {}, dataset {}", doi, datasetKey, e);
      doiGenerator.failed(doi, e);
    }
  }
}
