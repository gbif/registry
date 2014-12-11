package org.gbif.registry.ws.util;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.model.registry.eml.geospatial.GeospatialCoverage;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DateType;
import org.gbif.doi.metadata.datacite.DescriptionType;
import org.gbif.doi.metadata.datacite.ResourceType;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

import com.beust.jcommander.internal.Sets;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.apache.commons.lang.time.DateFormatUtils;

public class DataCiteConverter {

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
      .withPublicationYear(String.valueOf(getYear(new Date())))
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
      b.withPublicationYear(String.valueOf(getYear(d.getModified())))
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
      b.withPublicationYear(String.valueOf(getYear(d.getPubDate())));
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
  protected static Integer getYear(Date date) {
    if (date == null) {
      return null;
    }
    Calendar cal = new GregorianCalendar();
    cal.setTime(date);
    return cal.get(Calendar.YEAR);
  }
}
