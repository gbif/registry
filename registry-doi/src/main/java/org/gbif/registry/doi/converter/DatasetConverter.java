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
import org.gbif.doi.metadata.datacite.DataCiteMetadata.AlternateIdentifiers;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.AlternateIdentifiers.AlternateIdentifier;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Contributors;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Contributors.Contributor;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Contributors.Contributor.ContributorName;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator.CreatorName;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Dates;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Descriptions;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Descriptions.Description;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.GeoLocations;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.GeoLocations.GeoLocation;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Identifier;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Publisher;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.RightsList;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.RightsList.Rights;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Subjects;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles.Title;
import org.gbif.doi.metadata.datacite.DateType;
import org.gbif.doi.metadata.datacite.DescriptionType;
import org.gbif.doi.metadata.datacite.NameIdentifier;
import org.gbif.doi.metadata.datacite.ResourceType;
import org.gbif.registry.metadata.contact.ContactAdapter;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import static org.gbif.registry.doi.util.DataCiteConstants.ORCID_NAME_IDENTIFIER_SCHEME;
import static org.gbif.registry.doi.util.RegistryDoiUtils.fdate;
import static org.gbif.registry.doi.util.RegistryDoiUtils.getYear;

public final class DatasetConverter {

  private static final Map<ContactType, ContributorType> REGISTRY_DATACITE_ROLE_MAPPING =
      ImmutableMap.<ContactType, ContributorType>builder()
          .put(ContactType.EDITOR, ContributorType.EDITOR)
          .put(ContactType.PUBLISHER, ContributorType.EDITOR)
          .put(ContactType.CONTENT_PROVIDER, ContributorType.DATA_COLLECTOR)
          .put(ContactType.CUSTODIAN_STEWARD, ContributorType.DATA_MANAGER)
          .put(ContactType.CURATOR, ContributorType.DATA_CURATOR)
          .put(ContactType.METADATA_AUTHOR, ContributorType.DATA_CURATOR)
          .put(ContactType.DISTRIBUTOR, ContributorType.DISTRIBUTOR)
          .put(ContactType.OWNER, ContributorType.RIGHTS_HOLDER)
          .put(ContactType.POINT_OF_CONTACT, ContributorType.CONTACT_PERSON)
          .put(ContactType.PRINCIPAL_INVESTIGATOR, ContributorType.PROJECT_LEADER)
          .put(ContactType.ORIGINATOR, ContributorType.DATA_COLLECTOR)
          .put(ContactType.PROCESSOR, ContributorType.PRODUCER)
          .put(ContactType.PROGRAMMER, ContributorType.PRODUCER)
          .build();

  // Patterns must return 2 groups: scheme and id
  private static final Map<Pattern, String> SUPPORTED_SCHEMES =
      ImmutableMap.of(
          Pattern.compile("^(http[s]?://orcid.org/)([\\d\\-]+$)"), ORCID_NAME_IDENTIFIER_SCHEME);

  private DatasetConverter() {}

  /**
   * Convert a dataset and publisher object into a datacite metadata instance. DataCite requires at
   * least the following properties:
   *
   * <ul>
   *   <li>Identifier
   *   <li>Creators
   *   <li>Titles
   *   <li>Publisher
   *   <li>PublicationYear
   *   <li>ResourceType
   * </ul>
   *
   * As the publicationYear property is often not available from newly created datasets, this
   * converter uses the current year as the default in case no created timestamp or pubdate exists.
   */
  public static DataCiteMetadata convert(Dataset dataset, Organization publisher) {
    final DataCiteMetadata.Builder<Void> builder = DataCiteMetadata.builder();

    // Required fields
    convertIdentifier(builder, dataset);
    convertCreators(builder, dataset);
    convertTitles(builder, dataset);
    convertPublisher(builder, publisher);
    convertPublicationYear(builder, dataset);
    convertResourceType(builder, dataset);

    // Optional and recommended fields
    convertDates(builder, dataset);
    convertDescriptions(builder, dataset);
    convertLanguage(builder, dataset);
    convertContributors(builder, dataset);
    convertAlternateIdentifiers(builder, dataset);
    convertRelatedIdentifiers(builder);
    convertRightsList(builder, dataset);
    convertSubjects(builder, dataset);
    convertGeoLocations(builder, dataset);

    return builder.build();
  }

  private static void convertGeoLocations(DataCiteMetadata.Builder<Void> builder, Dataset dataset) {
    final GeoLocations.Builder<Void> geoLocationsBuilder = GeoLocations.builder();
    for (GeospatialCoverage gc : dataset.getGeographicCoverages()) {
      if (gc.getBoundingBox() != null) {
        // build geo locations
        builder.withGeoLocations(
            geoLocationsBuilder
                .addGeoLocation(
                    GeoLocation.builder()
                        .addGeoLocationPlace(gc.getDescription())
                        .addGeoLocationBox(
                            Box.builder()
                                .withEastBoundLongitude(
                                    (float) gc.getBoundingBox().getMaxLongitude())
                                .withNorthBoundLatitude(
                                    (float) gc.getBoundingBox().getMaxLatitude())
                                .withSouthBoundLatitude(
                                    (float) gc.getBoundingBox().getMinLatitude())
                                .withWestBoundLongitude(
                                    (float) gc.getBoundingBox().getMinLongitude())
                                .build())
                        .build())
                .build());
      }
    }
  }

  private static void convertSubjects(DataCiteMetadata.Builder<Void> builder, Dataset dataset) {
    final Subjects.Builder<Void> subjectsBuilder = Subjects.builder();
    for (KeywordCollection kCol : dataset.getKeywordCollections()) {
      for (String k : kCol.getKeywords()) {
        if (!Strings.isNullOrEmpty(k)) {
          Subjects.Subject s = Subjects.Subject.builder().withValue(k).build();
          if (!Strings.isNullOrEmpty(kCol.getThesaurus())) {
            s.setSubjectScheme(kCol.getThesaurus());
          }
          builder.withSubjects(subjectsBuilder.addSubject(s).build());
        }
      }
    }
  }

  private static void convertRightsList(DataCiteMetadata.Builder<Void> builder, Dataset dataset) {
    if (dataset.getLicense() != null && dataset.getLicense().isConcrete()) {
      builder.withRightsList(
          RightsList.builder()
              .withRights(
                  Rights.builder()
                      .withRightsURI(dataset.getLicense().getLicenseUrl())
                      .withValue(dataset.getLicense().getLicenseTitle())
                      .build())
              .build());
    } else {
      // this is still require for metadata only resource
      if (!Strings.isNullOrEmpty(dataset.getRights())) {
        builder.withRightsList(
            RightsList.builder()
                .withRights(Rights.builder().withValue(dataset.getRights()).build())
                .build());
      }
    }
  }

  private static void convertRelatedIdentifiers(DataCiteMetadata.Builder<Void> builder) {
    // empty list of RelatedIdentifiers is expected but callers
    builder.withRelatedIdentifiers().end();
  }

  private static void convertLanguage(DataCiteMetadata.Builder<Void> builder, Dataset dataset) {
    if (dataset.getDataLanguage() != null) {
      builder.withLanguage(dataset.getDataLanguage().getIso3LetterCode());
    } else {
      builder.withLanguage(dataset.getLanguage().getIso3LetterCode());
    }
  }

  private static void convertDescriptions(DataCiteMetadata.Builder<Void> builder, Dataset dataset) {
    if (!Strings.isNullOrEmpty(dataset.getDescription())) {
      builder.withDescriptions(
          Descriptions.builder()
              .addDescription(
                  Description.builder()
                      .withContent(dataset.getDescription())
                      .withDescriptionType(DescriptionType.ABSTRACT)
                      .build())
              .build());
    }
  }

  private static void convertDates(DataCiteMetadata.Builder<Void> builder, Dataset dataset) {
    final Dates.Builder<Void> datesBuilder = Dates.builder();
    if (dataset.getCreated() != null) {
      builder.withDates(
          datesBuilder
              .addDate(
                  Dates.Date.builder()
                      .withDateType(DateType.CREATED)
                      .withValue(fdate(dataset.getCreated()))
                      .build())
              .build());
    }

    if (dataset.getModified() != null) {
      builder.withDates(
          datesBuilder
              .addDate(
                  Dates.Date.builder()
                      .withDateType(DateType.UPDATED)
                      .withValue(fdate(dataset.getModified()))
                      .build())
              .build());
    }
  }

  private static void convertAlternateIdentifiers(
      DataCiteMetadata.Builder<Void> builder, Dataset dataset) {
    if (dataset.getDoi() != null && dataset.getKey() != null) {
      builder.withAlternateIdentifiers(
          AlternateIdentifiers.builder()
              .addAlternateIdentifier(
                  AlternateIdentifier.builder()
                      .withAlternateIdentifierType(IdentifierType.UUID.name())
                      .withValue(dataset.getKey().toString())
                      .build())
              .build());
    }
  }

  private static void convertIdentifier(DataCiteMetadata.Builder<Void> builder, Dataset dataset) {
    if (dataset.getDoi() != null) {
      builder.withIdentifier(
          Identifier.builder()
              .withIdentifierType(IdentifierType.DOI.name())
              .withValue(dataset.getDoi().getDoiName())
              .build());
    } else if (dataset.getKey() != null) {
      builder.withIdentifier(
          Identifier.builder()
              .withIdentifierType(IdentifierType.UUID.name())
              .withValue(dataset.getKey().toString())
              .build());
    }
  }

  private static void convertContributors(DataCiteMetadata.Builder<Void> builder, Dataset dataset) {
    if (dataset.getContacts() != null && !dataset.getContacts().isEmpty()) {
      ContactAdapter contactAdapter = new ContactAdapter(dataset.getContacts());

      List<Contact> resourceCreators = contactAdapter.getCreators();
      List<Contact> contributors = Lists.newArrayList(dataset.getContacts());
      contributors.removeAll(resourceCreators);

      final Contributors.Builder<Void> contributorsBuilder = Contributors.builder();
      if (!contributors.isEmpty()) {
        for (Contact contact : contributors) {
          toDataCiteContributor(contact)
              .ifPresent(
                  contributor ->
                      builder.withContributors(
                          contributorsBuilder.addContributor(contributor).build()));
        }
      }
    }
  }

  private static void convertCreators(DataCiteMetadata.Builder<Void> builder, Dataset dataset) {
    boolean creatorFound = false;
    if (dataset.getContacts() != null && !dataset.getContacts().isEmpty()) {
      ContactAdapter contactAdapter = new ContactAdapter(dataset.getContacts());

      List<Contact> resourceCreators = contactAdapter.getCreators();
      final Creators.Builder<Void> creatorsBuilder = Creators.builder();
      for (Contact resourceCreator : resourceCreators) {
        final Optional<Creator> creatorWrapper = toDataCiteCreator(resourceCreator);
        creatorFound = creatorFound ? creatorFound : creatorWrapper.isPresent();
        creatorWrapper.ifPresent(
            creator -> builder.withCreators(creatorsBuilder.addCreator(creator).build()));
      }
    }

    if (!creatorFound) {
      // creator is mandatory, build a default one
      builder.withCreators(
          Creators.builder()
              .addCreator(getDefaultGBIFDataCiteCreator(dataset.getCreatedBy()))
              .build());
    }
  }

  private static void convertResourceType(DataCiteMetadata.Builder<Void> builder, Dataset dataset) {
    builder.withResourceType(
        DataCiteMetadata.ResourceType.builder()
            .withResourceTypeGeneral(ResourceType.DATASET)
            .withValue(dataset.getType().name())
            .build());
  }

  private static void convertPublicationYear(
      DataCiteMetadata.Builder<Void> builder, Dataset dataset) {
    if (dataset.getPubDate() != null) {
      // use pub date for publication year if it exists
      builder.withPublicationYear(getYear(dataset.getPubDate()));
    } else if (dataset.getModified() != null) {
      builder.withPublicationYear(getYear(dataset.getModified()));
    } else {
      // default to this year, e.g. when creating new datasets. This field is required!
      builder.withPublicationYear(getYear(new Date()));
    }
  }

  private static void convertPublisher(
      DataCiteMetadata.Builder<Void> builder, Organization publisher) {
    builder.withPublisher(Publisher.builder().withValue(publisher.getTitle()).build());
  }

  private static void convertTitles(DataCiteMetadata.Builder<Void> builder, Dataset dataset) {
    builder.withTitles(
        Titles.builder().withTitle(Title.builder().withValue(dataset.getTitle()).build()).build());
  }

  /**
   * Transforms a Contact into a Datacite Creator.
   *
   * @return Creator instance or null if it is not possible to build one
   */
  private static Optional<Creator> toDataCiteCreator(Contact contact) {
    final String creatorNameValue = ContactAdapter.formatContactName(contact);

    // CreatorName is mandatory
    if (Strings.isNullOrEmpty(creatorNameValue)) {
      return Optional.empty();
    }

    final Creator.Builder<Void> creatorBuilder = Creator.builder();

    creatorBuilder.withCreatorName(CreatorName.builder().withValue(creatorNameValue).build());

    // affiliation is optional
    if (!Strings.isNullOrEmpty(contact.getOrganization())) {
      creatorBuilder.withAffiliation(
          Affiliation.builder().withValue(contact.getOrganization()).build());
    }

    NameIdentifier nId = userIdToNameIdentifier(contact.getUserId());
    if (nId != null) {
      creatorBuilder.withNameIdentifier(nId);
    }

    return Optional.of(creatorBuilder.build());
  }

  /** Transforms a Contact into a DataCite Creator. */
  private static Creator getDefaultGBIFDataCiteCreator(String fullname) {
    return Creator.builder()
        .withCreatorName(CreatorName.builder().withValue(fullname).build())
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
  private static Optional<Contributor> toDataCiteContributor(Contact contact) {
    final String nameValue = ContactAdapter.formatContactName(contact);

    // CreatorName is mandatory
    if (Strings.isNullOrEmpty(nameValue)) {
      return Optional.empty();
    }

    final Contributor.Builder<Void> contributorBuilder = Contributor.builder();
    contributorBuilder
        .withContributorName(ContributorName.builder().withValue(nameValue).build())
        .withContributorType(
            REGISTRY_DATACITE_ROLE_MAPPING.getOrDefault(
                contact.getType(), ContributorType.RELATED_PERSON));

    // affiliation is optional
    if (!Strings.isNullOrEmpty(contact.getOrganization())) {
      contributorBuilder.withAffiliation(
          Affiliation.builder().withValue(contact.getOrganization()).build());
    }

    NameIdentifier nId = userIdToNameIdentifier(contact.getUserId());
    if (nId != null) {
      contributorBuilder.withNameIdentifier(nId);
    }

    return Optional.of(contributorBuilder.build());
  }

  /**
   * Get the first userId from the list and transforms an userId in the form of
   * https://orcid.org/0000-0000-0000-00001 into a DataCite NameIdentifier object.
   *
   * @return a NameIdentifier instance or null if the object can not be built (e.g. unsupported
   *     scheme)
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
}
