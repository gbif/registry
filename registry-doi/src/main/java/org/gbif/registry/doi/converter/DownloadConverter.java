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

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.PredicateDownloadRequest;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.License;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.AlternateIdentifiers;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.AlternateIdentifiers.AlternateIdentifier;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator.CreatorName;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Descriptions;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Descriptions.Description;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Formats;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Identifier;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Publisher;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.RelatedIdentifiers;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.RelatedIdentifiers.RelatedIdentifier;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.RightsList;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.RightsList.Rights;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Sizes;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles.Title;
import org.gbif.doi.metadata.datacite.DateType;
import org.gbif.doi.metadata.datacite.DescriptionType;
import org.gbif.doi.metadata.datacite.NameType;
import org.gbif.doi.metadata.datacite.RelatedIdentifierType;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.doi.metadata.datacite.ResourceType;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.occurrence.query.HumanPredicateBuilder;
import org.gbif.occurrence.query.TitleLookupService;

import java.net.URI;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import static org.gbif.registry.doi.util.DataCiteConstants.DEFAULT_DOWNLOAD_LICENSE;
import static org.gbif.registry.doi.util.DataCiteConstants.DOWNLOAD_TITLE;
import static org.gbif.registry.doi.util.DataCiteConstants.DWCA_FORMAT;
import static org.gbif.registry.doi.util.DataCiteConstants.ENGLISH;
import static org.gbif.registry.doi.util.DataCiteConstants.GBIF_PUBLISHER;
import static org.gbif.registry.doi.util.DataCiteConstants.LICENSE_INFO;
import static org.gbif.registry.doi.util.RegistryDoiUtils.fdate;
import static org.gbif.registry.doi.util.RegistryDoiUtils.getYear;

public final class DownloadConverter {

  private DownloadConverter() {}

  /** Convert a download and its dataset usages into a datacite metadata instance. */
  public static DataCiteMetadata convert(
      Download download,
      GbifUser creator,
      List<DatasetOccurrenceDownloadUsage> usedDatasets,
      TitleLookupService titleLookup) {
    Preconditions.checkNotNull(
        download.getDoi(), "Download DOI required to build valid DOI metadata");
    Preconditions.checkNotNull(
        download.getCreated(), "Download created date required to build valid DOI metadata");
    Preconditions.checkNotNull(creator, "Download creator required to build valid DOI metadata");
    Preconditions.checkNotNull(
        download.getRequest(), "Download request required to build valid DOI metadata");

    DataCiteMetadata.Builder<Void> builder = DataCiteMetadata.builder();

    // Required fields
    convertIdentifier(builder, download);
    convertCreators(builder, creator);
    convertTitles(builder);
    convertPublisher(builder);
    convertPublicationYear(builder, download);
    convertResourceType(builder);

    // Optional and recommended fields
    convertDates(builder, download);
    convertDescriptions(builder, download, usedDatasets, titleLookup);
    convertAlternateIdentifiers(builder, download);
    convertRelatedIdentifiers(builder, usedDatasets);
    convertRightsList(builder, download);
    convertSubjects(builder);
    convertFormats(builder);
    convertSizes(builder, download);

    return builder.build();
  }

  private static void convertSizes(DataCiteMetadata.Builder<Void> builder, Download download) {
    builder.withSizes(Sizes.builder().addSize(Long.toString(download.getSize())).build());
  }

  private static void convertFormats(DataCiteMetadata.Builder<Void> builder) {
    builder.withFormats(Formats.builder().withFormat(DWCA_FORMAT).build());
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
      DataCiteMetadata.Builder<Void> builder, List<DatasetOccurrenceDownloadUsage> usedDatasets) {
    builder.withRelatedIdentifiers(
        getRelatedIdentifiersDatasetOccurrenceDownloadUsage(usedDatasets));
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
      TitleLookupService titleLookup) {
    builder.withDescriptions(
        Descriptions.builder()
            .addDescription(
                Description.builder()
                    .withDescriptionType(DescriptionType.ABSTRACT)
                    .withLang(ENGLISH)
                    .addContent(
                        String.format(
                            "A dataset containing %s species occurrences available in GBIF matching the query:\n%s\n\n",
                            download.getTotalRecords(), getFilterQuery(download, titleLookup)))
                    .addContent(
                        String.format(
                            "The dataset includes %s records from %s constituent datasets:\n",
                            download.getTotalRecords(), download.getNumberDatasets()))
                    .addContent(getDescriptionDatasetOccurrenceDownloadUsage(usedDatasets))
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

  private static DataCiteMetadata truncateDescriptionDCM(DOI doi, String xml, URI target)
      throws InvalidMetadataException {
    try {
      final DataCiteMetadata dm = DataCiteValidator.fromXml(xml);
      final String description =
          Joiner.on("\n").join(dm.getDescriptions().getDescription().get(0).getContent());
      final String truncatedDescriptionContent =
          StringUtils.substringBefore(description, "constituent datasets:")
              + String.format(
                  "constituent datasets:\nPlease see %s for full list of all constituents.",
                  target);

      final Descriptions truncatedDescription =
          Descriptions.builder()
              .withDescription(
                  Description.builder()
                      .withDescriptionType(DescriptionType.ABSTRACT)
                      .withLang(ENGLISH)
                      .withContent(truncatedDescriptionContent)
                      .build())
              .build();

      dm.setDescriptions(truncatedDescription);

      return dm;
    } catch (JAXBException e) {
      throw new InvalidMetadataException("Failed to deserialize datacite xml for DOI " + doi, e);
    }
  }

  public static String truncateDescription(DOI doi, String xml, URI target)
      throws InvalidMetadataException {
    DataCiteMetadata dm = truncateDescriptionDCM(doi, xml, target);
    return DataCiteValidator.toXml(doi, dm);
  }

  /** Removes all constituent relations and description entries from the metadata. */
  public static String truncateConstituents(DOI doi, String xml, URI target)
      throws InvalidMetadataException {
    DataCiteMetadata dm = truncateDescriptionDCM(doi, xml, target);
    // also remove constituent relations
    dm.setRelatedIdentifiers(null);
    return DataCiteValidator.toXml(doi, dm);
  }

  private static String getDescriptionDatasetOccurrenceDownloadUsage(
      List<DatasetOccurrenceDownloadUsage> usedDatasets) {
    final StringBuilder result = new StringBuilder();

    if (!usedDatasets.isEmpty()) {
      for (DatasetOccurrenceDownloadUsage du : usedDatasets) {
        if (!Strings.isNullOrEmpty(du.getDatasetTitle())) {
          result
              .append(" ")
              .append(du.getNumberRecords())
              .append(" records from ")
              .append(du.getDatasetTitle())
              .append(".\n");
        }
      }
      result.append("\n");
      result.append(LICENSE_INFO);
    }

    return result.toString();
  }

  private static RelatedIdentifiers getRelatedIdentifiersDatasetOccurrenceDownloadUsage(
      List<DatasetOccurrenceDownloadUsage> usedDatasets) {
    final RelatedIdentifiers.Builder relatedIdentifiersBuilder = RelatedIdentifiers.builder();
    if (!usedDatasets.isEmpty()) {
      for (DatasetOccurrenceDownloadUsage du : usedDatasets) {
        if (du.getDatasetDOI() != null) {
          relatedIdentifiersBuilder.addRelatedIdentifier(
              RelatedIdentifier.builder()
                  .withRelationType(RelationType.REFERENCES)
                  .withValue(du.getDatasetDOI().getDoiName())
                  .withRelatedIdentifierType(RelatedIdentifierType.DOI)
                  .build());
        }
      }
    }

    return relatedIdentifiersBuilder.build();
  }

  /**
   * Tries to get the human readable version of the download query, if fails returns the raw query.
   */
  private static String getFilterQuery(Download d, TitleLookupService titleLookup) {
    try {
      return new HumanPredicateBuilder(titleLookup)
          .humanFilterString(((PredicateDownloadRequest) d.getRequest()).getPredicate());
    } catch (Exception e) {
      return "(Query is too complex. Can be viewed on the landing page)";
    }
  }
}
