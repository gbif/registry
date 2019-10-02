package org.gbif.registry.doi.converter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadFormat;
import org.gbif.api.model.occurrence.PredicateDownloadRequest;
import org.gbif.api.model.occurrence.SqlDownloadRequest;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.License;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Descriptions;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Descriptions.Description;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.RelatedIdentifiers;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.RelatedIdentifiers.RelatedIdentifier;
import org.gbif.doi.metadata.datacite.DateType;
import org.gbif.doi.metadata.datacite.DescriptionType;
import org.gbif.doi.metadata.datacite.NameType;
import org.gbif.doi.metadata.datacite.RelatedIdentifierType;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.doi.metadata.datacite.ResourceType;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.occurrence.query.HumanPredicateBuilder;
import org.gbif.occurrence.query.TitleLookup;

import javax.xml.bind.JAXBException;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.gbif.registry.doi.DataCiteConstants.DEFAULT_DOWNLOAD_LICENSE;
import static org.gbif.registry.doi.DataCiteConstants.DOWNLOAD_TITLE;
import static org.gbif.registry.doi.DataCiteConstants.DWCA_FORMAT;
import static org.gbif.registry.doi.DataCiteConstants.ENGLISH;
import static org.gbif.registry.doi.DataCiteConstants.GBIF_PUBLISHER;
import static org.gbif.registry.doi.DataCiteConstants.LICENSE_INFO;

public final class DownloadConverter {

  private DownloadConverter() {
  }

  /**
   * Convert a download and its dataset usages into a datacite metadata instance.
   */
  public static DataCiteMetadata convert(Download d,
                                         GbifUser creator,
                                         List<DatasetOccurrenceDownloadUsage> usedDatasets,
                                         TitleLookup titleLookup) {
    Preconditions.checkNotNull(d.getDoi(), "Download DOI required to build valid DOI metadata");
    Preconditions.checkNotNull(d.getCreated(), "Download created date required to build valid DOI metadata");
    Preconditions.checkNotNull(creator, "Download creator required to build valid DOI metadata");
    Preconditions.checkNotNull(d.getRequest(), "Download request required to build valid DOI metadata");

    // always add required metadata
    DataCiteMetadata.Builder<Void> b = DataCiteMetadata.builder();

    // build identifier
    b.withIdentifier(DataCiteMetadata.Identifier.builder()
      .withIdentifierType(IdentifierType.DOI.name())
      .withValue(d.getDoi().getDoiName())
      .build());

    // build titles
    b.withTitles(DataCiteMetadata.Titles.builder()
      .withTitle(DataCiteMetadata.Titles.Title.builder()
        .withValue(DOWNLOAD_TITLE)
        .build())
      .build());

    // build subjects
    b.withSubjects(DataCiteMetadata.Subjects.builder()
      .addSubject(DataCiteMetadata.Subjects.Subject.builder()
        .withValue("GBIF")
        .withLang(ENGLISH)
        .build())
      .addSubject(DataCiteMetadata.Subjects.Subject.builder()
        .withValue("biodiversity")
        .withLang(ENGLISH)
        .build())
      .addSubject(DataCiteMetadata.Subjects.Subject.builder()
        .withValue("species occurrences")
        .withLang(ENGLISH)
        .build())
      .build());

    // build creators
    b.withCreators(DataCiteMetadata.Creators.builder()
      .addCreator(DataCiteMetadata.Creators.Creator.builder()
        .withCreatorName(DataCiteMetadata.Creators.Creator.CreatorName.builder()
          .withNameType(NameType.ORGANIZATIONAL)
          .withValue(creator.getName())
          .build())
        .build())
      .build());

    // build publisher
    b.withPublisher(DataCiteMetadata.Publisher.builder()
      .withValue(GBIF_PUBLISHER)
      .build());

    // build publication year
    b.withPublicationYear(getYear(d.getCreated()));

    // build resource type
    b.withResourceType(DataCiteMetadata.ResourceType.builder()
      .withResourceTypeGeneral(ResourceType.DATASET)
      .build());

    // build alternate identifiers
    b.withAlternateIdentifiers(DataCiteMetadata.AlternateIdentifiers.builder()
      .addAlternateIdentifier(DataCiteMetadata.AlternateIdentifiers.AlternateIdentifier.builder()
        .withAlternateIdentifierType("GBIF")
        .withValue(d.getKey())
        .build())
      .build());

    // build dates
    b.withDates(DataCiteMetadata.Dates.builder()
      .addDate(DataCiteMetadata.Dates.Date.builder()
        .withDateType(DateType.CREATED)
        .withValue(fdate(d.getCreated()))
        .build())
      .addDate(DataCiteMetadata.Dates.Date.builder()
        .withDateType(DateType.UPDATED)
        .withValue(fdate(d.getModified()))
        .build())
      .build());

    // build formats & sizes
    b.withFormats(
      DataCiteMetadata.Formats.builder()
        .withFormat(DWCA_FORMAT)
        .build());
    b.withSizes(
      DataCiteMetadata.Sizes.builder()
        .addSize(Long.toString(d.getSize()))
        .build());

    License downloadLicense = d.getLicense() != null && d.getLicense().isConcrete() ?
      d.getLicense() : DEFAULT_DOWNLOAD_LICENSE;

    // build rights list
    b.withRightsList(
      DataCiteMetadata.RightsList.builder()
        .addRights(
          DataCiteMetadata.RightsList.Rights.builder()
            .withRightsURI(downloadLicense.getLicenseUrl())
            .withValue(downloadLicense.getLicenseTitle())
            .build())
        .build());

    // build descriptions
    b.withDescriptions(
      Descriptions.builder()
        .addDescription(
          Description.builder()
            .withDescriptionType(DescriptionType.ABSTRACT)
            .withLang(ENGLISH)
            .addContent(String.format("A dataset containing %s species occurrences available in GBIF matching the query:\n%s\n\n",
              d.getTotalRecords(), getFilterQuery(d, titleLookup)))
            .addContent(String.format("The dataset includes %s records from %s constituent datasets:\n",
              d.getTotalRecords(), d.getNumberDatasets()))
            .addContent(getDescriptionDatasetOccurrenceDownloadUsage(usedDatasets))
            .build())
        .build());

    // build related identifiers
    b.withRelatedIdentifiers(getRelatedIdentifiersDatasetOccurrenceDownloadUsage(usedDatasets));

    return b.build();
  }

  private static DataCiteMetadata truncateDescriptionDCM(DOI doi, String xml, URI target) throws InvalidMetadataException {
    try {
      final DataCiteMetadata dm = DataCiteValidator.fromXml(xml);
      final String description = Joiner.on("\n").join(dm.getDescriptions().getDescription().get(0).getContent());
      final String truncatedDescriptionContent = StringUtils.substringBefore(description, "constituent datasets:") +
        String.format("constituent datasets:\nPlease see %s for full list of all constituents.", target);

      final Descriptions truncatedDescription = Descriptions.builder()
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

  public static String truncateDescription(DOI doi, String xml, URI target) throws InvalidMetadataException {
    DataCiteMetadata dm = truncateDescriptionDCM(doi, xml, target);
    return DataCiteValidator.toXml(doi, dm);
  }

  /**
   * Removes all constituent relations and description entries from the metadata.
   */
  public static String truncateConstituents(DOI doi, String xml, URI target) throws InvalidMetadataException {
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
          result.append(" ")
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
              .build()
          );
        }
      }
    }

    return relatedIdentifiersBuilder.build();
  }

  private static String fdate(Date date) {
    return DateFormatUtils.ISO_DATE_FORMAT.format(date);
  }

  @VisibleForTesting
  protected static String getYear(Date date) {
    if (date == null) {
      return null;
    }
    Calendar cal = new GregorianCalendar();
    cal.setTime(date);
    return String.valueOf(cal.get(Calendar.YEAR));
  }

  /**
   * Tries to get the human readable version of the download query, if fails returns the raw query.
   */
  private static String getFilterQuery(Download d, TitleLookup titleLookup) {
    try {
      return d.getRequest().getFormat().equals(DownloadFormat.SQL) ? ((SqlDownloadRequest) d.getRequest()).getSql()
        : new HumanPredicateBuilder(titleLookup).humanFilterString(((PredicateDownloadRequest) d.getRequest()).getPredicate());
    } catch (Exception e) {
      return "(Query is too complex. Can be viewed on the landing page)";
    }
  }
}
