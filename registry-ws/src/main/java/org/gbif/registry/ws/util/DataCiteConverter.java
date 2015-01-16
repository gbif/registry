package org.gbif.registry.ws.util;

import org.gbif.api.model.common.User;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.model.registry.eml.geospatial.GeospatialCoverage;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.Language;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DateType;
import org.gbif.doi.metadata.datacite.DescriptionType;
import org.gbif.doi.metadata.datacite.RelatedIdentifierType;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.doi.metadata.datacite.ResourceType;
import org.gbif.occurrence.query.HumanFilterBuilder;
import org.gbif.occurrence.query.TitleLookup;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import com.beust.jcommander.internal.Sets;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.lang.time.DateFormatUtils;

public class DataCiteConverter {

  private static final String DOWNLOAD_TITLE = "GBIF Occurrence Download %s";
  private static final String GBIF_PUBLISHER = "The Global Biodiversity Information Facility";
  private static final String RIGHTS = "CC0 1.0 Universal";
  private static final String RIGHTS_URL = "http://creativecommons.org/publicdomain/zero/1.0/";
  private static final String ENGLISH = Language.ENGLISH.getIso3LetterCode();
  private static final String DWAC_FORMAT = "Darwin Core Archive";

  private static String fdate(Date date) {
    return DateFormatUtils.ISO_DATE_FORMAT.format(date);
  }

  /**
   * Convert a dataset and publisher object into a datacite metadata instance.
   * DataCite requires at least the following properties:
   * <ul>
   *   <li>Identifier</li>
   *   <li>Creator</li>
   *   <li>Title</li>
   *   <li>Publisher</li>
   *   <li>PublicationYear</li>
   * </ul>
   *
   * As the publicationYear property is often not available from newly created datasets, this converter uses the current
   * year as the default in case no created timestamp or pubdate exists.
   */
  public static DataCiteMetadata convert(Dataset d, Organization publisher) {
    // always add required metadata
    DataCiteMetadata.Builder<java.lang.Void> b = DataCiteMetadata.builder()
      .withTitles().withTitle(DataCiteMetadata.Titles.Title.builder().withValue(d.getTitle()).build()).end()
      .withPublisher(publisher.getTitle())
      // default to this year, e.g. when creating new datasets. This field is required!
      .withPublicationYear(getYear(new Date()))
      .withResourceType().withResourceTypeGeneral(ResourceType.DATASET).withValue(d.getType().name()).end()
      .withCreators()
        .addCreator()
          .withCreatorName(publisher.getTitle())
          .withNameIdentifier()
            .withValue(publisher.getKey().toString())
            .withSchemeURI("gbif.org")
            .withNameIdentifierScheme("GBIF")
          .end()
        .end()
      .end()
      .withRelatedIdentifiers().end();

    if (d.getCreated() != null) {
      b.withPublicationYear(getYear(d.getModified()))
        .withDates()
        .addDate().withDateType(DateType.CREATED).withValue(fdate(d.getCreated())).end()
        .addDate().withDateType(DateType.UPDATED).withValue(fdate(d.getModified())).end()
        .end()
        .withCreators()
          .addCreator()
            .withCreatorName(d.getCreatedBy())
            .withNameIdentifier()
              .withValue(d.getCreatedBy())
              .withSchemeURI("gbif.org")
              .withNameIdentifierScheme("GBIF")
            .end()
          .end()
        .end();
    }
    if (d.getPubDate() != null) {
      // use pub date for publication year if it exists
      b.withPublicationYear(getYear(d.getPubDate()));
    }
    if (d.getModified() != null) {
      b.withDates()
        .addDate().withDateType(DateType.UPDATED).withValue(fdate(d.getModified()));
    }
    if (d.getDoi() != null) {
      b.withIdentifier().withIdentifierType(IdentifierType.DOI.name()).withValue(d.getDoi().getDoiName());
      if (d.getKey() != null) {
        b.withAlternateIdentifiers()
          .addAlternateIdentifier().withAlternateIdentifierType("UUID").withValue(d.getKey().toString());
      }
    } else if (d.getKey() != null) {
      b.withIdentifier().withIdentifierType("UUID").withValue(d.getKey().toString());
    }

    if (!Strings.isNullOrEmpty(d.getDescription())) {
      b.withDescriptions()
        .addDescription()
          .addContent(d.getDescription())
        .withDescriptionType(DescriptionType.ABSTRACT);
    }
    if (d.getDataLanguage() != null) {
      b.withLanguage(d.getDataLanguage().getTitleEnglish());
    }
    if (!Strings.isNullOrEmpty(d.getRights())) {
      b.withRightsList().addRights().withValue(d.getRights()).end();
    }
    Set<DataCiteMetadata.Subjects.Subject> subjects = Sets.newHashSet();
    for (KeywordCollection kcol : d.getKeywordCollections()) {
      for (String k : kcol.getKeywords()) {
        if (!Strings.isNullOrEmpty(k)) {
          DataCiteMetadata.Subjects.Subject s = DataCiteMetadata.Subjects.Subject.builder().withValue(k).build();
          if (!Strings.isNullOrEmpty(kcol.getThesaurus())) {
            s.setSubjectScheme(kcol.getThesaurus());
          }
          subjects.add(s);
        }
      }
    }
    for (GeospatialCoverage gc : d.getGeographicCoverages()) {
      if (gc.getBoundingBox() != null) {
        b.withGeoLocations().addGeoLocation().addGeoLocationBox(
          gc.getBoundingBox().getMinLatitude(),
          gc.getBoundingBox().getMinLongitude(),
          gc.getBoundingBox().getMaxLatitude(),
          gc.getBoundingBox().getMaxLongitude()
        );
      }
    }
    return b.build();
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
   * Convert a download and its dataset usages into a datacite metadata instance.
   */
  public static DataCiteMetadata convert(Download d, User creator, List<DatasetOccurrenceDownloadUsage> usedDatasets,
    TitleLookup titleLookup) {
    Preconditions.checkNotNull(d.getDoi(), "Download DOI required to build valid DOI metadata");
    Preconditions.checkNotNull(d.getCreated(), "Download created date required to build valid DOI metadata");
    Preconditions.checkNotNull(creator, "Download creator required to build valid DOI metadata");
    Preconditions.checkNotNull(d.getRequest(), "Download request required to build valid DOI metadata");

    // always add required metadata
    DataCiteMetadata.Builder<java.lang.Void> b = DataCiteMetadata.builder()
      .withIdentifier().withIdentifierType(IdentifierType.DOI.name()).withValue(d.getDoi().getDoiName()).end()
      .withTitles()
        .withTitle(
          DataCiteMetadata.Titles.Title.builder().withValue(String.format(DOWNLOAD_TITLE, d.getRequest().getCreator()))
            .build())
      .end()
      .withSubjects()
        .addSubject().withValue("GBIF").withLang(ENGLISH).end()
        .addSubject().withValue("biodiversity").withLang(ENGLISH).end()
        .addSubject().withValue("species occurrences").withLang(ENGLISH).end()
      .end()
      .withCreators().addCreator().withCreatorName(creator.getName()).end()
      .end()
      .withPublisher(GBIF_PUBLISHER).withPublicationYear(getYear(d.getCreated()))
      .withResourceType().withResourceTypeGeneral(ResourceType.DATASET).end()
      .withAlternateIdentifiers()
        .addAlternateIdentifier().withAlternateIdentifierType("GBIF").withValue(d.getKey()).end()
      .end().withDates().addDate().withDateType(DateType.CREATED).withValue(fdate(d.getCreated())).end()
        .addDate().withDateType(DateType.UPDATED).withValue(fdate(d.getModified())).end()
      .end().withFormats().addFormat(DWAC_FORMAT).end().withSizes().addSize(Long.toString(d.getSize())).end()
      .withRightsList()
      .addRights(DataCiteMetadata.RightsList.Rights.builder().withValue(RIGHTS).withRightsURI(RIGHTS_URL).build()).end();

    String query = new HumanFilterBuilder(titleLookup).humanFilterString(d.getRequest().getPredicate());
    final DataCiteMetadata.Descriptions.Description.Builder db = b.withDescriptions()
      .addDescription().withDescriptionType(DescriptionType.ABSTRACT).withLang(ENGLISH)
        .addContent(String.format("A dataset containing %s species occurrences available in GBIF matching the query: %s.",
            d.getTotalRecords(), query))
        .addContent(String.format("The dataset includes %s records from the following %s constituent datasets:",
          d.getTotalRecords(), d.getNumberDatasets()));

    if (!usedDatasets.isEmpty()) {
      for (DatasetOccurrenceDownloadUsage du : usedDatasets) {
        if(du.getDatasetDOI() != null) {
          b.withRelatedIdentifiers()
            .addRelatedIdentifier()
              .withRelationType(RelationType.REFERENCES)
              .withValue(du.getDatasetDOI().getDoiName())
              .withRelatedIdentifierType(RelatedIdentifierType.DOI)
            .end();
        }
        if (!Strings.isNullOrEmpty(du.getDatasetTitle())) {
          db.addContent("\n " + du.getNumberRecords() + " records from " + du.getDatasetTitle() + ".");
        }
      }
    }

    return b.build();
  }
}
