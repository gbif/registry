package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.User;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.model.registry.eml.geospatial.GeospatialCoverage;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.doi.metadata.datacite.ContributorType;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DateType;
import org.gbif.doi.metadata.datacite.DescriptionType;
import org.gbif.doi.metadata.datacite.ObjectFactory;
import org.gbif.doi.metadata.datacite.RelatedIdentifierType;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.doi.metadata.datacite.ResourceType;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.occurrence.query.HumanFilterBuilder;
import org.gbif.occurrence.query.TitleLookup;
import org.gbif.registry.metadata.contact.ContactAdapter;

import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataCiteConverter {

  private static final Logger LOG = LoggerFactory.getLogger(DataCiteConverter.class);

  private static final ObjectFactory FACTORY = new ObjectFactory();

  public static final String ORCID_NAME_IDENTIFIER_SCHEME = "ORCID";
  public static final String RESEARCHERID_NAME_IDENTIFIER_SCHEME = "ResearcherID";

  private static Map<ContactType, ContributorType> REGISTRY_DATACITE_ROLE_MAPPING =
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
          Pattern.compile("^(http[s]?:\\/\\/orcid.org\\/)([\\d\\-]+$)"), ORCID_NAME_IDENTIFIER_SCHEME);

  private static final String DOWNLOAD_TITLE = "GBIF Occurrence Download";
  private static final String GBIF_PUBLISHER = "The Global Biodiversity Information Facility";
  private static final License DEFAULT_DOWNLOAD_LICENSE = License.CC_BY_NC_4_0;
  private static final String LICENSE_INFO = "Data from some individual datasets included in this download may be licensed under less restrictive terms.";
  private static final String ENGLISH = Language.ENGLISH.getIso3LetterCode();
  private static final String DWCA_FORMAT = "Darwin Core Archive";

  private static String fdate(Date date) {
    return DateFormatUtils.ISO_DATE_FORMAT.format(date);
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
   * </ul>
   * As the publicationYear property is often not available from newly created datasets, this converter uses the
   * current
   * year as the default in case no created timestamp or pubdate exists.
   */
  public static DataCiteMetadata convert(Dataset d, Organization publisher) {
    // always add required metadata
    DataCiteMetadata.Builder<Void> b = DataCiteMetadata.builder()
            .withTitles().withTitle(DataCiteMetadata.Titles.Title.builder().withValue(d.getTitle()).build()).end()
            .withPublisher(publisher.getTitle())
                    // default to this year, e.g. when creating new datasets. This field is required!
            .withPublicationYear(getYear(new Date()))
            .withResourceType().withResourceTypeGeneral(ResourceType.DATASET).withValue(d.getType().name()).end();

    if (d.getCreated() != null) {
      b.withPublicationYear(getYear(d.getModified()))
              .withDates()
              .addDate().withDateType(DateType.CREATED).withValue(fdate(d.getCreated())).end()
              .addDate().withDateType(DateType.UPDATED).withValue(fdate(d.getModified())).end()
              .end();
    }

    // handle contacts
    if(d.getContacts() != null && !d.getContacts().isEmpty()){
      ContactAdapter contactAdapter = new ContactAdapter(d.getContacts());

      //handle Creators
      List<Contact> resourceCreators = contactAdapter.getCreators();
      DataCiteMetadata.Creators.Builder creatorsBuilder = b.withCreators();
      for(Contact resourceCreator : resourceCreators) {
        creatorsBuilder.addCreator(toDataCiteCreator(resourceCreator)).end();
      }
      creatorsBuilder.end();
      // creator is mandatory, build a default one
      if(resourceCreators.isEmpty()){
        b.withCreators().addCreator(getDefaultGBIFDataCiteCreator(d.getCreatedBy())).end().end();
      }

      //handle Contributors
      List<Contact> contributors = Lists.newArrayList(d.getContacts());
      contributors.removeAll(resourceCreators);

      if(!contributors.isEmpty()) {
        DataCiteMetadata.Contributors.Builder contributorsBuilder = b.withContributors();
        for (Contact contributor : contributors) {
          contributorsBuilder.addContributor(toDataCiteContributor(contributor)).end();
        }
        contributorsBuilder.end();
      }

    }
    else{ // creator is mandatory, build a default one
      b.withCreators().addCreator(getDefaultGBIFDataCiteCreator(d.getCreatedBy())).end().end();
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
      b.withLanguage(d.getDataLanguage().getIso3LetterCode());
    }

    if (d.getLicense() != null && d.getLicense().isConcrete()) {
      b.withRightsList().addRights()
              .withRightsURI(d.getLicense().getLicenseUrl()).withValue(d.getLicense().getLicenseTitle());
    } else {
      //this is still require for metadata only resource
      if (!Strings.isNullOrEmpty(d.getRights())) {
        b.withRightsList().addRights().withValue(d.getRights()).end();
      }
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

  private static DataCiteMetadata truncateDescriptionDCM(DOI doi, String xml, URI target) throws InvalidMetadataException {
    try {
      DataCiteMetadata dm = DataCiteValidator.fromXml(xml);
      String description = Joiner.on("\n").join(dm.getDescriptions().getDescription().get(0).getContent());
      dm.setDescriptions(DataCiteMetadata.Descriptions.builder().addDescription()
                      .withDescriptionType(DescriptionType.ABSTRACT)
                      .withLang(ENGLISH)
                      .addContent(StringUtils.substringBefore(description, "constituent datasets:") +
                              String.format("constituent datasets:\nPlease see %s for full list of all constituents.", target))
                      .end()
                      .build()
      );
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
    DataCiteMetadata.Builder<Void> b = DataCiteMetadata.builder()
            .withIdentifier().withIdentifierType(IdentifierType.DOI.name()).withValue(d.getDoi().getDoiName()).end()
            .withTitles()
            .withTitle(
                    DataCiteMetadata.Titles.Title.builder().withValue(DOWNLOAD_TITLE).build())
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
            .end().withFormats().addFormat(DWCA_FORMAT).end().withSizes().addSize(Long.toString(d.getSize())).end();

    License downloadLicense = d.getLicense() != null && d.getLicense().isConcrete() ? d.getLicense() : DEFAULT_DOWNLOAD_LICENSE;
    b.withRightsList().addRights()
              .withRightsURI(downloadLicense.getLicenseUrl()).withValue(downloadLicense.getLicenseTitle()).end();

    final DataCiteMetadata.Descriptions.Description.Builder db = b.withDescriptions()
            .addDescription().withDescriptionType(DescriptionType.ABSTRACT).withLang(ENGLISH)
            .addContent(String.format("A dataset containing %s species occurrences available in GBIF matching the query: %s.",
                    d.getTotalRecords(), getFilterQuery(d, titleLookup)))
            .addContent(String.format("The dataset includes %s records from %s constituent datasets:",
                    d.getTotalRecords(), d.getNumberDatasets()));
    if (!usedDatasets.isEmpty()) {
      final DataCiteMetadata.RelatedIdentifiers.Builder<?> relBuilder = b.withRelatedIdentifiers();
      for (DatasetOccurrenceDownloadUsage du : usedDatasets) {
        if (du.getDatasetDOI() != null) {
          relBuilder.addRelatedIdentifier()
                  .withRelationType(RelationType.REFERENCES)
                  .withValue(du.getDatasetDOI().getDoiName())
                  .withRelatedIdentifierType(RelatedIdentifierType.DOI)
                  .end();
        }
        if (!Strings.isNullOrEmpty(du.getDatasetTitle())) {
          db.addContent("\n " + du.getNumberRecords() + " records from " + du.getDatasetTitle() + ".");
        }
      }
      db.addContent(LICENSE_INFO);
    }

    return b.build();
  }

  /**
   * Transforms a Contact into a Datacite Creator.
   *
   * @param contact
   * @return
   */
  private static DataCiteMetadata.Creators.Creator toDataCiteCreator(Contact contact){
    DataCiteMetadata.Creators.Creator creator = FACTORY.createDataCiteMetadataCreatorsCreator();
    creator.setCreatorName(ContactAdapter.formatContactName(contact));
    // affiliation is optional
    if (!Strings.isNullOrEmpty(contact.getOrganization())) {
      creator.getAffiliation().add(contact.getOrganization());
    }

    for(String userId : contact.getUserId()){
      DataCiteMetadata.Creators.Creator.NameIdentifier nId = userIdToCreatorNameIdentifier(userId);
      if(nId != null){
        creator.setNameIdentifier(nId);
        //we take the first we can support
        break;
      }
    }
    return creator;
  }

  /**
   * Transforms a Contact into a Datacite Creator.
   * @param fullname
   * @return
   */
  private static DataCiteMetadata.Creators.Creator getDefaultGBIFDataCiteCreator(String fullname){
    DataCiteMetadata.Creators.Creator creator = FACTORY.createDataCiteMetadataCreatorsCreator();
    creator.setCreatorName(fullname);
    DataCiteMetadata.Creators.Creator.NameIdentifier nid =
            FACTORY.createDataCiteMetadataCreatorsCreatorNameIdentifier();
    nid.setValue(fullname);
    nid.setSchemeURI("gbif.org");
    nid.setNameIdentifierScheme("GBIF");
    creator.setNameIdentifier(nid);
    return creator;
  }

  /**
   * Transforms a Contact into a Datacite Contributor.
   *
   * @param contact
   * @return
   */
  private static DataCiteMetadata.Contributors.Contributor toDataCiteContributor(Contact contact){
    DataCiteMetadata.Contributors.Contributor contributor = FACTORY.createDataCiteMetadataContributorsContributor();
    contributor.setContributorName(ContactAdapter.formatContactName(contact));

    ContributorType contributorType = REGISTRY_DATACITE_ROLE_MAPPING.containsKey(contact.getType()) ?
            REGISTRY_DATACITE_ROLE_MAPPING.get(contact.getType()) : ContributorType.RELATED_PERSON;
    contributor.setContributorType(contributorType);

    for(String userId : contact.getUserId()){
      DataCiteMetadata.Contributors.Contributor.NameIdentifier nId = userIdToContributorNameIdentifier(userId);
      if(nId != null){
        contributor.setNameIdentifier(nId);
        //we take the first we can support
        break;
      }
    }
    return contributor;
  }

  /**
   * Transforms an userId in the form of https://orcid.org/0000-0000-0000-00001 into a Datacite NameIdentifier object.
   * @param userId
   * @return a Creator.NameIdentifier instance or null if the object can not be built (e.g. unsupported scheme)
   */
  @VisibleForTesting
  protected static DataCiteMetadata.Creators.Creator.NameIdentifier userIdToCreatorNameIdentifier(String userId){
    if(Strings.isNullOrEmpty(userId)){
      return null;
    }

    for(Pattern pattern : SUPPORTED_SCHEMES.keySet()){
      Matcher matcher = pattern.matcher(userId);
      if(matcher.matches()){
        DataCiteMetadata.Creators.Creator.NameIdentifier nid =
                FACTORY.createDataCiteMetadataCreatorsCreatorNameIdentifier();
        // group 0 = the entire string
        nid.setSchemeURI(matcher.group(1));
        nid.setValue(matcher.group(2));
        nid.setNameIdentifierScheme(SUPPORTED_SCHEMES.get(pattern));
        return nid;
      }
    }
    return null;
  }

  /**
   * Transforms an userId in the form of https://orcid.org/0000-0000-0000-00001 into a Datacite NameIdentifier object.
   * @param userId
   * @return a Contributor.NameIdentifier instance or null if the object can not be built (e.g. unsupported scheme)
   */
  protected static DataCiteMetadata.Contributors.Contributor.NameIdentifier userIdToContributorNameIdentifier(String userId){
    if(Strings.isNullOrEmpty(userId)){
      return null;
    }

    for(Pattern pattern : SUPPORTED_SCHEMES.keySet()){
      Matcher matcher = pattern.matcher(userId);
      if(matcher.matches()){
        DataCiteMetadata.Contributors.Contributor.NameIdentifier nid =
                FACTORY.createDataCiteMetadataContributorsContributorNameIdentifier();
        // group 0 = the entire string
        nid.setSchemeURI(matcher.group(1));
        nid.setValue(matcher.group(2));
        nid.setNameIdentifierScheme(SUPPORTED_SCHEMES.get(pattern));
        return nid;
      }
    }
    return null;
  }

  /**
   * Tries to get the human readable version of the download query, if fails returns the raw query.
   */
  private static String getFilterQuery(Download d, TitleLookup titleLookup) {
    try {
      return new HumanFilterBuilder(titleLookup).humanFilterString(d.getRequest().getPredicate());
    } catch (Exception ex) {
      return d.getRequest().getPredicate().toString();
    }
  }
}
