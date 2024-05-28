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
package org.gbif.registry.doi.converter;

import com.google.common.base.Preconditions;
import jakarta.xml.bind.JAXBException;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadFormat;
import org.gbif.api.model.occurrence.PredicateDownloadRequest;
import org.gbif.api.model.occurrence.SqlDownloadRequest;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.License;
import org.gbif.doi.metadata.datacite.*;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.*;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.AlternateIdentifiers.AlternateIdentifier;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator.CreatorName;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Descriptions.Description;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.RelatedIdentifiers.RelatedIdentifier;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.RightsList.Rights;
import org.gbif.doi.metadata.datacite.ResourceType;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles.Title;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.occurrence.query.HumanPredicateBuilder;
import org.gbif.occurrence.query.TitleLookupService;

import java.util.List;

import static org.gbif.registry.doi.util.DataCiteConstants.*;
import static org.gbif.registry.doi.util.RegistryDoiUtils.fdate;
import static org.gbif.registry.doi.util.RegistryDoiUtils.getYear;

public final class DownloadConverter {

  private DownloadConverter() {}

  /** Convert a download and its dataset usages into a Datacite metadata instance. */
  public static DataCiteMetadata convert(
      Download download,
      GbifUser creator,
      List<DatasetOccurrenceDownloadUsage> usedDatasets,
      TitleLookupService titleLookup,
      String apiRoot) {
    Preconditions.checkNotNull(
        download.getDoi(), "Download DOI required to build valid DOI metadata");
    Preconditions.checkNotNull(
        download.getCreated(), "Download created date required to build valid DOI metadata");
    Preconditions.checkNotNull(creator, "Download creator required to build valid DOI metadata");
    Preconditions.checkNotNull(
        download.getRequest(), "Download request required to build valid DOI metadata");

    DataCiteMetadata.Builder<Void> builder = DataCiteMetadata.builder();

    String cleanApiRoot = apiRoot.replace("http:", "https:") + // Ensure HTTPS
      (apiRoot.endsWith("/") ? "" : "/");

    // Required fields
    convertIdentifier(builder, download);
    convertCreators(builder, creator);
    convertTitles(builder);
    convertPublisher(builder);
    convertPublicationYear(builder, download);
    convertResourceType(builder);

    // Optional and recommended fields
    convertDates(builder, download);
    convertDescriptions(builder, download, usedDatasets, titleLookup, cleanApiRoot);
    convertAlternateIdentifiers(builder, download);
    convertRelatedIdentifiers(builder, download, usedDatasets, cleanApiRoot);
    convertRightsList(builder, download);
    convertSubjects(builder);
    convertFormats(builder, download);
    convertSizes(builder, download);

    return builder.build();
  }

  private static void convertSizes(DataCiteMetadata.Builder<Void> builder, Download download) {
    builder.withSizes(Sizes.builder().addSize(Long.toString(download.getSize())).build());
  }

  private static void convertFormats(DataCiteMetadata.Builder<Void> builder, Download download) {
    DownloadFormat format = download.getRequest().getFormat();

    switch (format) {
      // Darwin core archive
      case DWCA:
        builder.withFormats(Formats.builder().withFormat(DWCA_FORMAT).addFormat(TSV_FORMAT).addFormat(ZIP_FORMAT).build());
        break;

      // Zipped TSV files
      case SPECIES_LIST:
      case SIMPLE_CSV:
      case SQL_TSV_ZIP:
        builder.withFormats(Formats.builder().withFormat(TSV_FORMAT).addFormat(ZIP_FORMAT).build());
        break;

      // Zipped Avro files
      case BIONOMIA:
      case MAP_OF_LIFE:
      case SIMPLE_WITH_VERBATIM_AVRO:
        builder.withFormats(Formats.builder().withFormat(AVRO_FORMAT).addFormat(ZIP_FORMAT).build());
        break;

      // Single Avro file
      case SIMPLE_AVRO:
        builder.withFormats(Formats.builder().withFormat(AVRO_FORMAT).build());
        break;

      // Parquet
      case SIMPLE_PARQUET:
        builder.withFormats(Formats.builder().withFormat(PARQUET_FORMAT).addFormat(ZIP_FORMAT).build());
        break;

      // Leave unspecified
      default:
    }
  }

  private static void convertRightsList(DataCiteMetadata.Builder<Void> builder, Download download) {
    License downloadLicense =
        download.getLicense() != null && download.getLicense().isConcrete()
            ? download.getLicense()
            : DEFAULT_DOWNLOAD_LICENSE;

    builder.withRightsList(
        RightsList.builder()
            .addRights(
                Rights.builder()
                    .withRightsURI(downloadLicense.getLicenseUrl())
                    .withValue(downloadLicense.getLicenseTitle())
                    .build())
            .build());
  }

  private static void convertRelatedIdentifiers(
      DataCiteMetadata.Builder<Void> builder, Download download, List<DatasetOccurrenceDownloadUsage> usedDatasets,
      String apiRoot) {
    builder.withRelatedIdentifiers(
        getRelatedIdentifiersDatasetOccurrenceDownloadUsage(usedDatasets, download, apiRoot));
  }

  private static void convertAlternateIdentifiers(
      DataCiteMetadata.Builder<Void> builder, Download download) {
    builder.withAlternateIdentifiers(
        AlternateIdentifiers.builder()
            .addAlternateIdentifier(
                AlternateIdentifier.builder()
                    .withAlternateIdentifierType("GBIF")
                    .withValue(download.getKey())
                    .build())
            .build());
  }

  private static void convertSubjects(DataCiteMetadata.Builder<Void> builder) {
    builder.withSubjects(
        DataCiteMetadata.Subjects.builder()
            .addSubject(
                DataCiteMetadata.Subjects.Subject.builder()
                    .withValue("GBIF")
                    .withLang(ENGLISH)
                    .build())
            .addSubject(
                DataCiteMetadata.Subjects.Subject.builder()
                    .withValue("biodiversity")
                    .withLang(ENGLISH)
                    .build())
            .addSubject(
                DataCiteMetadata.Subjects.Subject.builder()
                    .withValue("species occurrences")
                    .withLang(ENGLISH)
                    .build())
            .build());
  }

  private static void convertDescriptions(
      DataCiteMetadata.Builder<Void> builder,
      Download download,
      List<DatasetOccurrenceDownloadUsage> usedDatasets,
      TitleLookupService titleLookup,
      String apiRoot) {

    DownloadFormat format = download.getRequest().getFormat();

    String totalRecordsDescriptionFormat, constituentDatasetsDescriptionFormat;
    switch (format) {
      // Grouped summaries
      case SPECIES_LIST:
        totalRecordsDescriptionFormat = "A dataset listing the %s species recorded in GBIF matching the query:\n%s\n\n";
        constituentDatasetsDescriptionFormat = "The dataset's %s records were derived from %s constituent datasets; see "+API_DOWNLOAD_DATASETS_EXPORT_METADATA+" for details.\n";
        break;

      case SQL_TSV_ZIP:
        totalRecordsDescriptionFormat = "A dataset containing %s records in GBIF matching the query:\n%s\n\n";
        constituentDatasetsDescriptionFormat = "The dataset's %s records were derived from %s constituent datasets; see "+API_DOWNLOAD_DATASETS_EXPORT_METADATA+" for details.\n";
        break;

      // Plain species occurrence records
      case BIONOMIA:
      case DWCA:
      case MAP_OF_LIFE:
      case SIMPLE_AVRO:
      case SIMPLE_CSV:
      case SIMPLE_WITH_VERBATIM_AVRO:
      case SIMPLE_PARQUET:
      default:
        totalRecordsDescriptionFormat = "A dataset containing %s species occurrences available in GBIF matching the query:\n%s\n\n";
        constituentDatasetsDescriptionFormat = "The dataset includes %s records from %s constituent datasets; see "+API_DOWNLOAD_DATASETS_EXPORT_METADATA+" for details.\n";
    }

    builder.withDescriptions(
        Descriptions.builder()
            .addDescription(
                Description.builder()
                    .withDescriptionType(DescriptionType.ABSTRACT)
                    .withLang(ENGLISH)
                    .addContent(
                        String.format(totalRecordsDescriptionFormat, download.getTotalRecords(), getFilterQuery(download, titleLookup)))
                    .addContent(
                        String.format(
                            constituentDatasetsDescriptionFormat, download.getTotalRecords(), download.getNumberDatasets(), apiRoot, download.getKey()))
                    .addContent(usedDatasets.isEmpty() ? "" : "\n" + LICENSE_INFO)
                    .build())
            .build());
  }

  private static void convertDates(DataCiteMetadata.Builder<Void> builder, Download download) {
    builder.withDates(
        DataCiteMetadata.Dates.builder()
            .addDate(
                DataCiteMetadata.Dates.Date.builder()
                    .withDateType(DateType.CREATED)
                    .withValue(fdate(download.getCreated()))
                    .build())
            .addDate(
                DataCiteMetadata.Dates.Date.builder()
                    .withDateType(DateType.UPDATED)
                    .withValue(fdate(download.getModified()))
                    .build())
            .build());
  }

  private static void convertResourceType(DataCiteMetadata.Builder<Void> builder) {
    builder.withResourceType(
        DataCiteMetadata.ResourceType.builder()
            .withResourceTypeGeneral(ResourceType.DATASET)
            .build());
  }

  private static void convertPublicationYear(
      DataCiteMetadata.Builder<Void> builder, Download download) {
    builder.withPublicationYear(getYear(download.getCreated()));
  }

  private static void convertPublisher(DataCiteMetadata.Builder<Void> builder) {
    builder.withPublisher(Publisher.builder().withValue(GBIF_PUBLISHER).build());
  }

  private static void convertTitles(DataCiteMetadata.Builder<Void> builder) {
    builder.withTitles(
        Titles.builder().withTitle(Title.builder().withValue(DOWNLOAD_TITLE).build()).build());
  }

  private static void convertCreators(DataCiteMetadata.Builder<Void> builder, GbifUser creator) {
    builder.withCreators(
        Creators.builder()
            .addCreator(
                Creator.builder()
                    .withCreatorName(
                        CreatorName.builder()
                            .withNameType(NameType.ORGANIZATIONAL)
                            .withValue(creator.getName())
                            .build())
                    .build())
            .build());
  }

  private static void convertIdentifier(DataCiteMetadata.Builder<Void> builder, Download download) {
    builder.withIdentifier(
        Identifier.builder()
            .withIdentifierType(IdentifierType.DOI.name())
            .withValue(download.getDoi().getDoiName())
            .build());
  }

  /** Removes all constituent relations from the metadata, keeping the GBIF API metadata. */
  public static String truncateConstituents(DOI doi, String xml)
    throws InvalidMetadataException {
    try {
      final DataCiteMetadata dm = DataCiteValidator.fromXml(xml);
      final RelatedIdentifiers.Builder relatedIdentifiersBuilder = RelatedIdentifiers.builder();
      for (RelatedIdentifier identifier : dm.getRelatedIdentifiers().getRelatedIdentifier()) {
        if (RelationType.HAS_METADATA.equals(identifier.getRelationType())) {
          relatedIdentifiersBuilder.addRelatedIdentifier(identifier);
        }
      }
      dm.setRelatedIdentifiers(relatedIdentifiersBuilder.build());

      return DataCiteValidator.toXml(doi, dm);
    } catch (JAXBException e) {
      throw new InvalidMetadataException("Failed to deserialize DataCite XML for DOI " + doi, e);
    }
  }

  private static RelatedIdentifiers getRelatedIdentifiersDatasetOccurrenceDownloadUsage(
      List<DatasetOccurrenceDownloadUsage> usedDatasets, Download download, String apiRoot) {
    final RelatedIdentifiers.Builder relatedIdentifiersBuilder = RelatedIdentifiers.builder();
    if (!usedDatasets.isEmpty()) {
      for (DatasetOccurrenceDownloadUsage du : usedDatasets) {
        if (du.getDatasetDOI() != null) {
          relatedIdentifiersBuilder.addRelatedIdentifier(
              RelatedIdentifier.builder()
                  .withRelationType(RelationType.IS_DERIVED_FROM)
                  .withValue(du.getDatasetDOI().getDoiName())
                  .withRelatedIdentifierType(RelatedIdentifierType.DOI)
                  .build());
        }
      }
    }

    // Link to GBIF's API for additional metadata
    relatedIdentifiersBuilder.addRelatedIdentifier(RelatedIdentifier.builder()
      .withRelatedIdentifierType(RelatedIdentifierType.URL)
      .withRelationType(RelationType.HAS_METADATA)
      .withValue(String.format(API_DOWNLOAD_METADATA, apiRoot, download.getKey()))
      .build());
    relatedIdentifiersBuilder.addRelatedIdentifier(RelatedIdentifier.builder()
      .withRelatedIdentifierType(RelatedIdentifierType.URL)
      .withRelationType(RelationType.HAS_METADATA)
      .withValue(String.format(API_DOWNLOAD_DATASETS_METADATA, apiRoot, download.getKey()))
      .build());

    return relatedIdentifiersBuilder.build();
  }

  /**
   * Tries to get the human-readable version of the download query, if fails returns the raw query.
   *
   * For SQL downloads just returns the SQL.
   */
  private static String getFilterQuery(Download d, TitleLookupService titleLookup) {
    try {
      if (d.getRequest() instanceof PredicateDownloadRequest) {
        return new HumanPredicateBuilder(titleLookup)
          .humanFilterString(((PredicateDownloadRequest) d.getRequest()).getPredicate());
      } else if (d.getRequest() instanceof SqlDownloadRequest) {
        return ((SqlDownloadRequest) d.getRequest()).getSql();
      } else {
        return "(Query can be viewed on the landing page)";
      }
    } catch (Exception e) {
      return "(Query is too complex. Can be viewed on the landing page)";
    }
  }
}
