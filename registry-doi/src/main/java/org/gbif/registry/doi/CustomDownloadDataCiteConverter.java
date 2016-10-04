package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DateType;
import org.gbif.doi.metadata.datacite.DescriptionType;
import org.gbif.doi.metadata.datacite.RelatedIdentifierType;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.doi.metadata.datacite.ResourceType;

import java.util.Date;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Class that enables creation of a DataCite metadata instance for a custom download.
 */
public class CustomDownloadDataCiteConverter {

  private static final String CUSTOM_DOWNLOAD_TITLE = "GBIF Custom Occurrence Download";
  private static final String DOWNLOAD_FORMAT = "Compressed and UTF-8 encoded tab delimited file";

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
   *
   * @return DataCiteMetadata for a custom download
   */
  @VisibleForTesting
  public static DataCiteMetadata convert(DOI doi, String size, String numberRecords, String creatorName,
    String creatorUserId, Date created, List<DatasetOccurrenceDownloadUsage> usedDatasets) {
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
        .withSubjects().addSubject().withValue("GBIF").withLang(DataCiteConverter.ENGLISH).end().addSubject().withValue("biodiversity")
        .withLang(DataCiteConverter.ENGLISH).end().addSubject().withValue("species occurrences").withLang(DataCiteConverter.ENGLISH).end().end()
        .withCreators().addCreator().withCreatorName(creatorName)
        .withNameIdentifier(DataCiteConverter.userIdToCreatorNameIdentifier(creatorUserId)).end().end()
        .withPublisher(DataCiteConverter.GBIF_PUBLISHER).withPublicationYear(DataCiteConverter.getYear(created)).withResourceType()
        .withResourceTypeGeneral(ResourceType.DATASET).end().withDates().addDate().withDateType(DateType.CREATED)
        .withValue(DataCiteConverter.fdate(
          created)).end().addDate().withDateType(DateType.UPDATED).withValue(DataCiteConverter.fdate(created)).end().end()
        .withFormats().addFormat(DOWNLOAD_FORMAT).end().withSizes().addSize(size).end();

    // License always set to most restrictive (CC BY-NC 4.0)
    b.withRightsList().addRights().withRightsURI(DataCiteConverter.DEFAULT_DOWNLOAD_LICENSE.getLicenseUrl())
      .withValue(DataCiteConverter.DEFAULT_DOWNLOAD_LICENSE.getLicenseTitle()).end();

    // Add description detailing number of records used from each dataset used in download
    final DataCiteMetadata.Descriptions.Description.Builder db =
      b.withDescriptions().addDescription().withDescriptionType(DescriptionType.ABSTRACT)
        .withLang(DataCiteConverter.ENGLISH).addContent(String
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
