package org.gbif.registry.doi.converter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DateType;
import org.gbif.doi.metadata.datacite.DescriptionType;
import org.gbif.doi.metadata.datacite.RelatedIdentifierType;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.doi.metadata.datacite.ResourceType;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static org.gbif.registry.doi.DataCiteConstants.CUSTOM_DOWNLOAD_TITLE;
import static org.gbif.registry.doi.DataCiteConstants.DEFAULT_DOWNLOAD_LICENSE;
import static org.gbif.registry.doi.DataCiteConstants.DOWNLOAD_FORMAT;
import static org.gbif.registry.doi.DataCiteConstants.ENGLISH;
import static org.gbif.registry.doi.DataCiteConstants.GBIF_PUBLISHER;

/**
 * Class that enables creation of a DataCite metadata instance for a custom download.
 */
public final class CustomDownloadDataCiteConverter {

  private CustomDownloadDataCiteConverter() {
  }

  /**
   * Convert a custom download and its dataset usages into a DataCite metadata instance.
   *
   * @param doi           download DOI
   * @param size          download size in bytes
   * @param numberRecords number of species occurrence records in custom download
   * @param creatorName   creator name
   * @param creatorUserId creator identifier e.g. ORCID
   * @param created       date created
   * @param usedDatasets  list of datasets download is derived from
   * @return DataCiteMetadata for a custom download
   */
  // TODO: 02/10/2019 refactor
  @VisibleForTesting
  public static DataCiteMetadata convert(DOI doi, String size, String numberRecords, String creatorName,
                                         String creatorUserId, Calendar created, List<DatasetOccurrenceDownloadUsage> usedDatasets) {
    Preconditions.checkNotNull(doi, "Custom download DOI required to build valid DataCite metadata");
    Preconditions.checkNotNull(size, "Custom download size required to build valid DataCite metadata");
    Preconditions.checkNotNull(numberRecords, "Custom download record count required to build valid DataCite metadata");
    Preconditions.checkNotNull(creatorName, "Custom download creator required to build valid DataCite metadata");
    Preconditions.checkNotNull(creatorUserId, "Custom download creator ID required to build valid DataCite metadata");
    Preconditions.checkNotNull(created, "Custom download created date required to build valid DataCite metadata");
    Preconditions.checkArgument(usedDatasets.size() > 0, "Used datasets required to build valid DataCite metadata");

    DataCiteMetadata.Builder<Void> b =
      DataCiteMetadata.builder().withIdentifier().withIdentifierType(IdentifierType.DOI.name())
        .withValue(doi.getDoiName()).end().withTitles()
        .withTitle(DataCiteMetadata.Titles.Title.builder().withValue(CUSTOM_DOWNLOAD_TITLE).build()).end()
        .withSubjects().addSubject().withValue("GBIF").withLang(ENGLISH).end().addSubject().withValue("biodiversity")
        .withLang(ENGLISH).end().addSubject().withValue("species occurrences").withLang(
        ENGLISH).end().end()
        .withCreators().addCreator().withCreatorName().withValue(creatorName).end()
        .withNameIdentifier(DatasetConverter.userIdToNameIdentifier(Collections.singletonList(creatorUserId))).end().end()
        .withPublisher().withValue(GBIF_PUBLISHER).end()
        .withPublicationYear(String.valueOf(created.get(Calendar.YEAR)))
        .withResourceType().withResourceTypeGeneral(ResourceType.DATASET).end()
        .withDates()
        .addDate().withDateType(DateType.CREATED).withValue(DateFormatUtils.ISO_DATE_FORMAT.format(created)).end()
        .addDate().withDateType(DateType.UPDATED).withValue(DateFormatUtils.ISO_DATE_FORMAT.format(created)).end()
        .end()
        .withFormats().addFormat(DOWNLOAD_FORMAT).end().withSizes().addSize(size).end();

    // License always set to most restrictive (CC BY-NC 4.0)
    b.withRightsList().addRights().withRightsURI(DEFAULT_DOWNLOAD_LICENSE.getLicenseUrl())
      .withValue(DEFAULT_DOWNLOAD_LICENSE.getLicenseTitle()).end();

    // Add description detailing number of records used from each dataset used in download
    final DataCiteMetadata.Descriptions.Description.Builder db =
      b.withDescriptions().addDescription().withDescriptionType(DescriptionType.ABSTRACT)
        .withLang(ENGLISH).addContent(String
        .format("A custom GBIF download containing %s records derived from %s datasets:\n", numberRecords,
          usedDatasets.size()));

    // Add relations detailing all datasets download is derived from
    if (!usedDatasets.isEmpty()) {
      final DataCiteMetadata.RelatedIdentifiers.Builder<?> relBuilder = b.withRelatedIdentifiers();
      for (DatasetOccurrenceDownloadUsage du : usedDatasets) {
        if (du.getDatasetDOI() != null) {
          relBuilder.addRelatedIdentifier().withRelationType(RelationType.IS_DERIVED_FROM)
            .withValue(du.getDatasetDOI().getDoiName()).withRelatedIdentifierType(RelatedIdentifierType.DOI).end();
        }
        if (!Strings.isNullOrEmpty(du.getDatasetTitle())) {
          db.addContent(String.format("%s records from %s. %s\n", du.getNumberRecords(), du.getDatasetTitle(),
            du.getDatasetDOI().getUrl()));
        }
      }
      db.addContent(
        "Data from some individual datasets included in this download may be licensed under less restrictive terms.");
    }
    return b.build();
  }
}
