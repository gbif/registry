package org.gbif.registry.doi;

import com.google.common.collect.Lists;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.RelatedIdentifierType;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.occurrence.query.TitleLookupService;
import org.gbif.registry.doi.converter.DatasetConverter;
import org.gbif.registry.doi.converter.DerivedDatasetConverter;
import org.gbif.registry.doi.converter.DownloadConverter;
import org.gbif.registry.domain.ws.Citation;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.List;

@Service
public class DataCiteMetadataBuilderServiceImpl implements DataCiteMetadataBuilderService {

  // Page size to iterate over dataset usages
  private static final int USAGES_PAGE_SIZE = 400;

  private final OrganizationMapper organizationMapper;
  private final OccurrenceDownloadService occurrenceDownloadService;
  private final TitleLookupService titleLookupService;

  public DataCiteMetadataBuilderServiceImpl(
      OrganizationMapper organizationMapper,
      OccurrenceDownloadService occurrenceDownloadService,
      TitleLookupService titleLookupService) {
    this.organizationMapper = organizationMapper;
    this.occurrenceDownloadService = occurrenceDownloadService;
    this.titleLookupService = titleLookupService;
  }

  @Override
  public DataCiteMetadata buildMetadata(Citation citation) {
    return DerivedDatasetConverter.convert(citation);
  }

  @Override
  public DataCiteMetadata buildMetadata(Download download, GbifUser user) {
    List<DatasetOccurrenceDownloadUsage> response = null;
    List<DatasetOccurrenceDownloadUsage> usages = Lists.newArrayList();
    PagingRequest pagingRequest = new PagingRequest(0, USAGES_PAGE_SIZE);

    while (response == null || !response.isEmpty()) {
      response =
          occurrenceDownloadService
              .listDatasetUsages(download.getKey(), pagingRequest)
              .getResults();
      usages.addAll(response);
      pagingRequest.nextPage();
    }

    return DownloadConverter.convert(download, user, usages, titleLookupService);
  }

  @Override
  public DataCiteMetadata buildMetadata(Dataset dataset) {
    return buildMetadata(dataset, null, null);
  }

  @Override
  public DataCiteMetadata buildMetadata(
      Dataset dataset, @Nullable DOI related, @Nullable RelationType relationType) {
    Organization publisher = organizationMapper.get(dataset.getPublishingOrganizationKey());
    DataCiteMetadata m = DatasetConverter.convert(dataset, publisher);
    // add previous relationship
    if (related != null) {
      m.getRelatedIdentifiers()
          .getRelatedIdentifier()
          .add(
              DataCiteMetadata.RelatedIdentifiers.RelatedIdentifier.builder()
                  .withRelationType(relationType)
                  .withValue(related.getDoiName())
                  .withRelatedIdentifierType(RelatedIdentifierType.DOI)
                  .build());
    }
    return m;
  }
}
