package org.gbif.registry.doi.converter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.model.registry.eml.geospatial.GeospatialCoverage;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.doi.metadata.datacite.Affiliation;
import org.gbif.doi.metadata.datacite.Box;
import org.gbif.doi.metadata.datacite.ContributorType;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DateType;
import org.gbif.doi.metadata.datacite.DescriptionType;
import org.gbif.doi.metadata.datacite.NameIdentifier;
import org.gbif.doi.metadata.datacite.ResourceType;
import org.gbif.registry.metadata.contact.ContactAdapter;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.gbif.registry.doi.DataCiteConstants.ORCID_NAME_IDENTIFIER_SCHEME;

public final class DatasetConverter {

  private static final Map<ContactType, ContributorType> REGISTRY_DATACITE_ROLE_MAPPING =
    ImmutableMap.<ContactType, ContributorType>builder().
      put(ContactType.EDITOR, ContributorType.EDITOR).
      put(ContactType.PUBLISHER, ContributorType.EDITOR).
      put(ContactType.CONTENT_PROVIDER, ContributorType.DATA_COLLECTOR).
      put(ContactType.CUSTODIAN_STEWARD, ContributorType.DATA_MANAGER).
      put(ContactType.CURATOR, ContributorType.DATA_CURATOR).
      put(ContactType.METADATA_AUTHOR, ContributorType.DATA_CURATOR).
      put(ContactType.DISTRIBUTOR, ContributorType.DISTRIBUTOR).
      put(ContactType.OWNER, ContributorType.RIGHTS_HOLDER).
      put(ContactType.POINT_OF_CONTACT, ContributorType.CONTACT_PERSON).
      put(ContactType.PRINCIPAL_INVESTIGATOR, ContributorType.PROJECT_LEADER).
      put(ContactType.ORIGINATOR, ContributorType.DATA_COLLECTOR).
      put(ContactType.PROCESSOR, ContributorType.PRODUCER).
      put(ContactType.PROGRAMMER, ContributorType.PRODUCER).
      build();

  // Patterns must return 2 groups: scheme and id
  public static final Map<Pattern, String> SUPPORTED_SCHEMES = ImmutableMap.of(
    Pattern.compile("^(http[s]?://orcid.org/)([\\d\\-]+$)"), ORCID_NAME_IDENTIFIER_SCHEME);

  private DatasetConverter() {
  }

  /**
   * Convert a dataset and publisher object into a datacite metadata instance.
   * DataCite requires at least the following properties:
   * <ul>
   * <li>Identifier</li>
   * <li>Creator</li>
   * <li>Title</li>
   * <li>Publisher</li>
   * <li>PublicationYear</li>
   * <li>ResourceType</li>
   * </ul>
   * As the publicationYear property is often not available from newly created datasets, this converter uses the
   * current year as the default in case no created timestamp or pubdate exists.
   */
  // TODO: 01/10/2019 method is too complex
  public static DataCiteMetadata convert(Dataset d, Organization publisher) {
    // always add required metadata
    DataCiteMetadata.Builder<Void> b = DataCiteMetadata.builder()
      // build titles
      .withTitles(
        DataCiteMetadata.Titles.builder()
          .addTitle(
            DataCiteMetadata.Titles.Title.builder()
              .withValue(d.getTitle())
              .build())
          .build());

    // build publisher
    b.withPublisher(
      DataCiteMetadata.Publisher.builder()
        .withValue(publisher.getTitle())
        .build());

    // build publication year
    // default to this year, e.g. when creating new datasets. This field is required!
    b.withPublicationYear(getYear(new Date()));

    // build resource type
    b.withResourceType(
      DataCiteMetadata.ResourceType.builder()
        .withResourceTypeGeneral(ResourceType.DATASET)
        .withValue(d.getType().name())
        .build());

    // build related identifiers
    // empty list of RelatedIdentifiers is expected but callers
    b.withRelatedIdentifiers().end();

    final DataCiteMetadata.Dates.Builder<Void> datesBuilder = DataCiteMetadata.Dates.builder();
    if (d.getCreated() != null) {
      // build dates
      b.withDates(
        datesBuilder
          .addDate(
            DataCiteMetadata.Dates.Date.builder()
              .withDateType(DateType.CREATED)
              .withValue(fdate(d.getCreated()))
              .build())
          .build());
    }

    if (d.getModified() != null) {
      // build publication year
      b.withPublicationYear(getYear(d.getModified()));

      // build dates
      b.withDates(
        datesBuilder
          .addDate(
            DataCiteMetadata.Dates.Date.builder()
              .withDateType(DateType.UPDATED)
              .withValue(fdate(d.getModified()))
              .build())
          .build());
    }

    // handle contacts
    boolean creatorFound = false;
    if (d.getContacts() != null && !d.getContacts().isEmpty()) {
      ContactAdapter contactAdapter = new ContactAdapter(d.getContacts());

      // handle Creators
      List<Contact> resourceCreators = contactAdapter.getCreators();
      final DataCiteMetadata.Creators.Builder<Void> creatorsBuilder = DataCiteMetadata.Creators.builder();
      for (Contact resourceCreator : resourceCreators) {
        final Optional<DataCiteMetadata.Creators.Creator> creatorWrapper = toDataCiteCreator(resourceCreator);
        creatorFound = creatorFound ? creatorFound : creatorWrapper.isPresent();
        creatorWrapper
          // build creators
          .ifPresent(creator -> b.withCreators(
            creatorsBuilder
              .addCreator(creator)
              .build()
            )
          );
      }

      // handle Contributors
      List<Contact> contributors = Lists.newArrayList(d.getContacts());
      contributors.removeAll(resourceCreators);

      final DataCiteMetadata.Contributors.Builder<Void> contributorsBuilder = DataCiteMetadata.Contributors.builder();
      if (!contributors.isEmpty()) {
        for (Contact contact : contributors) {
          toDataCiteContributor(contact)
            // build contributors
            .ifPresent(contributor -> b.withContributors(
              contributorsBuilder
                .addContributor(contributor)
                .build()
              )
            );
        }
      }
    }

    if (!creatorFound) {
      // creator is mandatory, build a default one
      b.withCreators(
        DataCiteMetadata.Creators.builder()
          .addCreator(getDefaultGBIFDataCiteCreator(d.getCreatedBy()))
          .build());
    }

    if (d.getPubDate() != null) {
      // build publication year
      // use pub date for publication year if it exists
      b.withPublicationYear(getYear(d.getPubDate()));
    }

    if (d.getDoi() != null) {
      // build identifiers
      b.withIdentifier(
        DataCiteMetadata.Identifier.builder()
          .withIdentifierType(IdentifierType.DOI.name())
          .withValue(d.getDoi().getDoiName())
          .build());

      if (d.getKey() != null) {
        // build alternate identifiers
        b.withAlternateIdentifiers(
          DataCiteMetadata.AlternateIdentifiers.builder()
            .addAlternateIdentifier(
              DataCiteMetadata.AlternateIdentifiers.AlternateIdentifier.builder()
                .withAlternateIdentifierType("UUID")
                .withValue(d.getKey().toString())
                .build())
            .build());
      }
    } else if (d.getKey() != null) {
      // build identifier
      b.withIdentifier(
        DataCiteMetadata.Identifier.builder()
          .withIdentifierType(IdentifierType.UUID.name())
          .withValue(d.getKey().toString())
          .build());
    }

    if (!Strings.isNullOrEmpty(d.getDescription())) {
      // build description
      b.withDescriptions(
        DataCiteMetadata.Descriptions.builder()
          .addDescription(
            DataCiteMetadata.Descriptions.Description.builder()
              .withContent(d.getDescription())
              .withDescriptionType(DescriptionType.ABSTRACT)
              .build())
          .build());
    }

    if (d.getDataLanguage() != null) {
      // build language
      b.withLanguage(d.getDataLanguage().getIso3LetterCode());
    }

    // build rights list
    if (d.getLicense() != null && d.getLicense().isConcrete()) {
      b.withRightsList(
        DataCiteMetadata.RightsList.builder()
          .addRights(
            DataCiteMetadata.RightsList.Rights.builder()
              .withRightsURI(d.getLicense().getLicenseUrl())
              .withValue(d.getLicense().getLicenseTitle())
              .build())
          .build());
    } else {
      // this is still require for metadata only resource
      if (!Strings.isNullOrEmpty(d.getRights())) {
        b.withRightsList(
          DataCiteMetadata.RightsList.builder()
            .addRights(
              DataCiteMetadata.RightsList.Rights.builder()
                .withValue(d.getRights())
                .build())
            .build());
      }
    }

    // build subjects
    final DataCiteMetadata.Subjects.Builder<Void> subjectsBuilder = DataCiteMetadata.Subjects.builder();
    for (KeywordCollection kCol : d.getKeywordCollections()) {
      for (String k : kCol.getKeywords()) {
        if (!Strings.isNullOrEmpty(k)) {
          DataCiteMetadata.Subjects.Subject s = DataCiteMetadata.Subjects.Subject.builder().withValue(k).build();
          if (!Strings.isNullOrEmpty(kCol.getThesaurus())) {
            s.setSubjectScheme(kCol.getThesaurus());
          }
          b.withSubjects(subjectsBuilder
            .addSubject(s)
            .build());
        }
      }
    }

    final DataCiteMetadata.GeoLocations.Builder<Void> geoLocationsBuilder = DataCiteMetadata.GeoLocations.builder();
    for (GeospatialCoverage gc : d.getGeographicCoverages()) {
      if (gc.getBoundingBox() != null) {
        // build geo locations
        b.withGeoLocations(geoLocationsBuilder
          .addGeoLocation(DataCiteMetadata.GeoLocations.GeoLocation.builder()
            .addGeoLocationPlace(gc.getDescription())
            .addGeoLocationBox(Box.builder()
              .withEastBoundLongitude((float) gc.getBoundingBox().getMaxLongitude())
              .withNorthBoundLatitude((float) gc.getBoundingBox().getMaxLatitude())
              .withSouthBoundLatitude((float) gc.getBoundingBox().getMinLatitude())
              .withWestBoundLongitude((float) gc.getBoundingBox().getMinLongitude())
              .build())
            .build())
          .build());
      }
    }
    return b.build();
  }

  /**
   * Transforms a Contact into a Datacite Creator.
   *
   * @return Creator instance or null if it is not possible to build one
   */
  private static Optional<DataCiteMetadata.Creators.Creator> toDataCiteCreator(Contact contact) {
    final String creatorNameValue = ContactAdapter.formatContactName(contact);

    // CreatorName is mandatory
    if (Strings.isNullOrEmpty(creatorNameValue)) {
      return Optional.empty();
    }

    final DataCiteMetadata.Creators.Creator.Builder<Void> creatorBuilder = DataCiteMetadata.Creators.Creator.builder();

    creatorBuilder
      .withCreatorName(
        DataCiteMetadata.Creators.Creator.CreatorName.builder()
          .withValue(creatorNameValue)
          .build());

    // affiliation is optional
    if (!Strings.isNullOrEmpty(contact.getOrganization())) {
      creatorBuilder
        .withAffiliation(
          Affiliation.builder()
            .withValue(contact.getOrganization())
            .build());
    }

    NameIdentifier nId = userIdToNameIdentifier(contact.getUserId());
    if (nId != null) {
      creatorBuilder.withNameIdentifier(nId);
    }

    return Optional.of(creatorBuilder.build());
  }

  /**
   * Transforms a Contact into a DataCite Creator.
   */
  private static DataCiteMetadata.Creators.Creator getDefaultGBIFDataCiteCreator(String fullname) {
    return DataCiteMetadata.Creators.Creator.builder()
      .withCreatorName(
        DataCiteMetadata.Creators.Creator.CreatorName.builder()
          .withValue(fullname)
          .build())
      .withNameIdentifier(
        NameIdentifier.builder()
          .withValue(fullname)
          .withSchemeURI("gbif.org")
          .withNameIdentifierScheme("GBIF")
          .build())
      .build();
  }

  /**
   * Transforms a Contact into a Datacite Contributor.
   *
   * @return Contributor instance or null if it is not possible to build one
   */
  private static Optional<DataCiteMetadata.Contributors.Contributor> toDataCiteContributor(Contact contact) {
    final String nameValue = ContactAdapter.formatContactName(contact);

    // CreatorName is mandatory
    if (Strings.isNullOrEmpty(nameValue)) {
      return Optional.empty();
    }

    final DataCiteMetadata.Contributors.Contributor.Builder<Void> contributorBuilder = DataCiteMetadata.Contributors.Contributor.builder();
    contributorBuilder
      .withContributorName(
        DataCiteMetadata.Contributors.Contributor.ContributorName.builder()
          .withValue(nameValue)
          .build())
      .withContributorType(
        REGISTRY_DATACITE_ROLE_MAPPING.getOrDefault(contact.getType(), ContributorType.RELATED_PERSON)
      );

    // affiliation is optional
    if (!Strings.isNullOrEmpty(contact.getOrganization())) {
      contributorBuilder
        .withAffiliation(
          Affiliation.builder()
            .withValue(contact.getOrganization())
            .build());
    }

    NameIdentifier nId = userIdToNameIdentifier(contact.getUserId());
    if (nId != null) {
      contributorBuilder.withNameIdentifier(nId);
    }

    return Optional.of(contributorBuilder.build());
  }

  /**
   * Get the first userId from the list and
   * transforms an userId in the form of https://orcid.org/0000-0000-0000-00001 into a DataCite NameIdentifier object.
   *
   * @return a NameIdentifier instance or null if the object can not be built (e.g. unsupported scheme)
   */
  public static NameIdentifier userIdToNameIdentifier(List<String> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return null;
    }

    for (String userId : userIds) {
      if (Strings.isNullOrEmpty(userId)) {
        // try the next one
        continue;
      }

      for (Map.Entry<Pattern, String> scheme : SUPPORTED_SCHEMES.entrySet()) {
        Matcher matcher = scheme.getKey().matcher(userId);
        if (matcher.matches()) {
          // group 0 = the entire string
          // we take the first we can support
          return NameIdentifier.builder()
            .withSchemeURI(matcher.group(1))
            .withValue(matcher.group(2))
            .withNameIdentifierScheme(scheme.getValue())
            .build();
        }
      }
    }
    return null;
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
}
