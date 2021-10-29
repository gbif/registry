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
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator.CreatorName;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Dates;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Descriptions;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Descriptions.Description;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Formats;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Identifier;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.RelatedIdentifiers;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.RelatedIdentifiers.RelatedIdentifier;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Sizes;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Subjects;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Subjects.Subject;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles.Title;
import org.gbif.doi.metadata.datacite.DateType;
import org.gbif.doi.metadata.datacite.DescriptionType;
import org.gbif.doi.metadata.datacite.RelatedIdentifierType;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.doi.metadata.datacite.ResourceType;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.time.DateFormatUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import static org.gbif.registry.doi.util.DataCiteConstants.CUSTOM_DOWNLOAD_TITLE;
import static org.gbif.registry.doi.util.DataCiteConstants.DEFAULT_DOWNLOAD_LICENSE;
import static org.gbif.registry.doi.util.DataCiteConstants.DOWNLOAD_FORMAT;
import static org.gbif.registry.doi.util.DataCiteConstants.ENGLISH;
import static org.gbif.registry.doi.util.DataCiteConstants.GBIF_PUBLISHER;

/** Class that enables creation of a DataCite metadata instance for a custom download. */
public final class CustomDownloadDataCiteConverter {

  private CustomDownloadDataCiteConverter() {}

  /**
   * Convert a custom download and its dataset usages into a DataCite metadata instance.
   *
   * @param doi download DOI
   * @param size download size in bytes
   * @param numberRecords number of species occurrence records in custom download
   * @param creatorName creator name
   * @param creatorUserId creator identifier e.g. ORCID
   * @param created date created
   * @param usedDatasets list of datasets download is derived from
   * @return DataCiteMetadata for a custom download
   */
  public static DataCiteMetadata convert(
      DOI doi,
      String size,
      String numberRecords,
      String creatorName,
      String creatorUserId,
      Calendar created,
      List<DatasetOccurrenceDownloadUsage> usedDatasets) {
    Preconditions.checkNotNull(
        doi, "Custom download DOI required to build valid DataCite metadata");
    Preconditions.checkNotNull(
        size, "Custom download size required to build valid DataCite metadata");
    Preconditions.checkNotNull(
        numberRecords, "Custom download record count required to build valid DataCite metadata");
    Preconditions.checkNotNull(
        creatorName, "Custom download creator required to build valid DataCite metadata");
    Preconditions.checkNotNull(
        creatorUserId, "Custom download creator ID required to build valid DataCite metadata");
    Preconditions.checkNotNull(
        created, "Custom download created date required to build valid DataCite metadata");
    Preconditions.checkArgument(
        !usedDatasets.isEmpty(), "Used datasets required to build valid DataCite metadata");

    DataCiteMetadata.Builder<Void> builder = DataCiteMetadata.builder();

    // Required fields
    convertIdentifier(builder, doi);
    convertCreators(builder, creatorName, creatorUserId);
    convertTitles(builder);
    convertPublisher(builder);
    convertPublicationYear(builder, created);
    convertResourceType(builder);

    // Optional and recommended fields
    convertSubjects(builder);
    convertDates(builder, created);
    convertFormats(builder);
    convertSizes(builder, size);
    convertRightsList(builder);
    convertDescriptions(builder, numberRecords, usedDatasets);
    convertRelatedIdentifiers(builder, usedDatasets);

    return builder.build();
  }

  private static void convertRelatedIdentifiers(
      DataCiteMetadata.Builder<Void> builder, List<DatasetOccurrenceDownloadUsage> usedDatasets) {
    // Add relations detailing all datasets download is derived from
    if (!usedDatasets.isEmpty()) {
      final RelatedIdentifiers.Builder<?> relBuilder = builder.withRelatedIdentifiers();
      for (DatasetOccurrenceDownloadUsage du : usedDatasets) {
        if (du.getDatasetDOI() != null) {
          relBuilder.addRelatedIdentifier(
              RelatedIdentifier.builder()
                  .withRelationType(RelationType.IS_DERIVED_FROM)
                  .withValue(du.getDatasetDOI().getDoiName())
                  .withRelatedIdentifierType(RelatedIdentifierType.DOI)
                  .build());
        }
      }
    }
  }

  private static void convertDescriptions(
      DataCiteMetadata.Builder<Void> builder,
      String numberRecords,
      List<DatasetOccurrenceDownloadUsage> usedDatasets) {
    // Add description detailing number of records used from each dataset used in download
    Description.Builder<Void> descriptionBuilder = Description.builder();

    descriptionBuilder
        .withDescriptionType(DescriptionType.ABSTRACT)
        .withLang(ENGLISH)
        .withContent(
            String.format(
                "A custom GBIF download containing %s records derived from %s datasets:%n",
                numberRecords, usedDatasets.size()))
        .build();

    // Add an additional description information about used datasets
    if (!usedDatasets.isEmpty()) {
      for (DatasetOccurrenceDownloadUsage du : usedDatasets) {
        if (!Strings.isNullOrEmpty(du.getDatasetTitle())) {
          descriptionBuilder.addContent(
              String.format(
                  "%s records from %s. %s%n",
                  du.getNumberRecords(), du.getDatasetTitle(), du.getDatasetDOI().getUrl()));
        }
      }
      descriptionBuilder.addContent(
          "Data from some individual datasets included "
              + "in this download may be licensed under less restrictive terms.");
    }

    builder.withDescriptions(
        Descriptions.builder().addDescription(descriptionBuilder.build()).build());
  }

  private static void convertRightsList(DataCiteMetadata.Builder<Void> builder) {
    // License always set to most restrictive (CC BY-NC 4.0)
    builder.withRightsList(
        DataCiteMetadata.RightsList.builder()
            .withRights(
                DataCiteMetadata.RightsList.Rights.builder()
                    .withRightsURI(DEFAULT_DOWNLOAD_LICENSE.getLicenseUrl())
                    .withValue(DEFAULT_DOWNLOAD_LICENSE.getLicenseTitle())
                    .build())
            .build());
  }

  private static void convertSizes(DataCiteMetadata.Builder<Void> builder, String size) {
    builder.withSizes(Sizes.builder().withSize(size).build());
  }

  private static void convertFormats(DataCiteMetadata.Builder<Void> builder) {
    builder.withFormats(Formats.builder().withFormat(DOWNLOAD_FORMAT).build());
  }

  private static void convertDates(DataCiteMetadata.Builder<Void> builder, Calendar created) {
    builder.withDates(
        Dates.builder()
            .addDate(
                Dates.Date.builder()
                    .withDateType(DateType.CREATED)
                    .withValue(DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.format(created))
                    .build())
            .addDate(
                Dates.Date.builder()
                    .withDateType(DateType.UPDATED)
                    .withValue(DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.format(created))
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
      DataCiteMetadata.Builder<Void> builder, Calendar created) {
    builder.withPublicationYear(String.valueOf(created.get(Calendar.YEAR)));
  }

  private static void convertPublisher(DataCiteMetadata.Builder<Void> builder) {
    builder.withPublisher(DataCiteMetadata.Publisher.builder().withValue(GBIF_PUBLISHER).build());
  }

  private static void convertCreators(
      DataCiteMetadata.Builder<Void> builder, String creatorName, String creatorUserId) {
    builder.withCreators(
        Creators.builder()
            .withCreator(
                Creator.builder()
                    .withCreatorName(CreatorName.builder().withValue(creatorName).build())
                    .withNameIdentifier(
                        DatasetConverter.userIdToNameIdentifier(
                            Collections.singletonList(creatorUserId)))
                    .build())
            .build());
  }

  private static void convertSubjects(DataCiteMetadata.Builder<Void> builder) {
    builder.withSubjects(
        Subjects.builder()
            .addSubject(Subject.builder().withValue("GBIF").withLang(ENGLISH).build())
            .addSubject(Subject.builder().withValue("biodiversity").withLang(ENGLISH).build())
            .addSubject(
                Subject.builder().withValue("species occurrences").withLang(ENGLISH).build())
            .build());
  }

  private static void convertTitles(DataCiteMetadata.Builder<Void> builder) {
    builder.withTitles(
        Titles.builder()
            .withTitle(Title.builder().withValue(CUSTOM_DOWNLOAD_TITLE).build())
            .build());
  }

  private static void convertIdentifier(DataCiteMetadata.Builder<Void> builder, DOI doi) {
    builder.withIdentifier(
        Identifier.builder()
            .withIdentifierType(IdentifierType.DOI.name())
            .withValue(doi.getDoiName())
            .build());
  }
}
