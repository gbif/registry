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
package org.gbif.registry.metadata.parse;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.eml.Collection;
import org.gbif.api.model.registry.eml.DataDescription;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.model.registry.eml.Project;
import org.gbif.api.model.registry.eml.SamplingDescription;
import org.gbif.api.model.registry.eml.TaxonomicCoverage;
import org.gbif.api.model.registry.eml.TaxonomicCoverages;
import org.gbif.api.model.registry.eml.curatorial.CuratorialUnitComposite;
import org.gbif.api.model.registry.eml.geospatial.GeospatialCoverage;
import org.gbif.api.model.registry.eml.temporal.DateRange;
import org.gbif.api.model.registry.eml.temporal.TemporalCoverage;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.MaintenanceUpdateFrequency;
import org.gbif.api.vocabulary.PreservationMethodType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.common.parsers.LicenseParser;
import org.gbif.common.parsers.RankParser;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.common.parsers.date.DateParsers;
import org.gbif.registry.metadata.CleanUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A delegating wrapper to a Dataset that can be instructed to override existing content or not.
 * This allows an existing Dataset to be augmented by new content.
 *
 * <p>Warning: Apache Digester can(I can not confirm it is always) call the setter of a parent
 * object before the setter of nested objects. e.g. setCitation will be called before the
 * setIdentifier and setText on the Citation object.
 */
@SuppressWarnings("unused")
public class DatasetWrapper {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetWrapper.class);
  // matches UUID v4 + version like /v2.1
  private static final Pattern PACKAGE_ID_VERSION_PATTERN =
      Pattern.compile(
          "[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89aAbB][a-f0-9]{3}-[a-f0-9]{12}/v(\\d+.\\d+)");
  private final Dataset target = new Dataset();
  private final ParagraphContainer description = new ParagraphContainer();

  /**
   * Utility to parse an EML calendarDate in a textual format. Can be ISO date or just the year,
   * ignoring whitespace
   *
   * @param dateString To set in format YYYY-MM-DD or YYYY
   * @return the parsed date
   * @throws java.text.ParseException Should it be an erroneous format
   * @see <a
   *     href="http://knb.ecoinformatics.org/software/eml/eml-2.1.0/eml-coverage.html#calendarDate">EML
   *     Coverage calendarDate keyword</a>
   */
  private static Date calendarDate(String dateString) throws ParseException {
    if (StringUtils.isEmpty(dateString)) {
      return null;
    }

    ParseResult<TemporalAccessor> result =
        DateParsers.defaultNumericalDateParser().parse(dateString);
    if (result.getStatus() == ParseResult.STATUS.SUCCESS) {
      TemporalAccessor ta = result.getPayload();
      if (ta.isSupported(ChronoField.DAY_OF_MONTH)) {
        return Date.from(LocalDate.from(ta).atStartOfDay(ZoneOffset.UTC).toInstant());
      } else if (ta.isSupported(ChronoField.YEAR)) {
        return Date.from(
            Year.from(ta).atDay(1).atStartOfDay(ZoneOffset.UTC).plusNanos(1000).toInstant());
      }
    }

    return null;
  }

  public void addBibliographicCitation(Citation citation) {
    target.getBibliographicCitations().add(citation);
  }

  public void addCollection(Collection collection) {
    target.getCollections().add(collection);
  }

  /**
   * Add contact to target dataset.
   *
   * @param contact Contact
   */
  public void addContact(Contact contact) {
    CleanUtils.removeEmptyStrings(contact);
    if (verifyContact(contact)) {
      target.getContacts().add(contact);
    }
  }

  /** @return true if the minimal required contact information exists */
  private boolean verifyContact(Contact contact) {
    return contact.getFirstName() != null
        || contact.getLastName() != null
        || !contact.getPosition().isEmpty()
        || !contact.getEmail().isEmpty()
        || !contact.getPhone().isEmpty()
        || contact.getOrganization() != null
        || (!contact.getAddress().isEmpty() && contact.getCity() != null);
  }

  /**
   * We only ever expect a single set of curatorial units per Collection. The Curatorial Units are
   * outside the Collection element in the GBIF EML schema, so it's by strong assumption that the
   * units belong to the collection.
   *
   * @param curatorialUnit curatorial unit
   */
  public void addCuratorial(CuratorialUnitComposite curatorialUnit) {
    // make sure a collection exists
    if (target.getCollections().isEmpty()) {
      target.getCollections().add(new Collection());
    }
    // add
    target.getCollections().get(0).addCuratorialUnitComposite(curatorialUnit);
  }

  public void addDataDescription(DataDescription dataDescription) {
    target.getDataDescriptions().add(dataDescription);
  }

  public void addGeographicCoverage(GeospatialCoverage coverage) {
    target.getGeographicCoverages().add(coverage);
  }

  public void addIdentifier(Identifier identifier) {
    target.getIdentifiers().add(identifier);
  }

  public void addKeywordCollection(KeywordCollection collection) {
    target.getKeywordCollections().add(collection);
  }

  /** Adds a comma or semicolon concatenated keyword string as keyword collection. */
  public void addSubjects(String subjects) {
    if (StringUtils.isNotEmpty(subjects)) {
      KeywordCollection collection = new KeywordCollection();

      Arrays.stream(subjects.split("[,;]"))
          .map(org.gbif.utils.text.StringUtils::trim)
          .filter(StringUtils::isNotEmpty)
          .forEach(collection::addKeyword);

      target.getKeywordCollections().add(collection);
    }
  }

  public void addCreator(String creator) {
    if (StringUtils.isNotEmpty(creator)) {
      Contact contact = new Contact();
      contact.setLastName(creator);
      contact.setType(ContactType.ORIGINATOR);
      addContact(contact);
    }
  }

  public void addBibCitation(String citation) {
    if (StringUtils.isNotEmpty(citation)) {
      Citation c = new Citation();
      c.setText(citation);
      target.setCitation(c);
    }
  }

  public void addIdentifier(String id) {
    if (StringUtils.isNotEmpty(id)) {
      Identifier i = new Identifier();
      i.setIdentifier(id);
      i.setType(IdentifierType.UNKNOWN);
      target.getIdentifiers().add(i);
    }
  }

  public void addDescription(ParagraphContainer para) {
    if (para != null) {
      target.setDescription(para.toString());
    }
  }

  public void addAbstract(String para) {
    if (StringUtils.isNotEmpty(para)) {
      description.appendParagraph(para.trim());
      target.setDescription(description.toString());
    }
  }

  public void addDataUrl(URI uri) {
    if (uri != null) {
      DataDescription d = new DataDescription();
      d.setUrl(uri);
      target.getDataDescriptions().add(d);
    }
  }

  /**
   * Similar to addContact() except that it sets type to ADMINISTRATIVE_POINT_OF_CONTACT, and sets
   * isPrimary flag to true only if this is the first contact of type
   * ADMINISTRATIVE_POINT_OF_CONTACT in dataset's contact list.
   *
   * @param contact Contact
   */
  public void addPreferredAdministrativeContact(Contact contact) {
    boolean primaryExists =
        isPrimaryExisting(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT, target.getContacts());
    contact.setPrimary(!primaryExists);
    // set type to administrative
    contact.setType(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT);
    addContact(contact);
  }

  /**
   * Similar to addContact() except that it sets type to METADATA_AUTHOR, and sets isPrimary flag to
   * true only if this is the first contact of type METADATA_AUTHOR in dataset's contact list.
   *
   * @param contact Contact
   */
  public void addPreferredMetadataContact(Contact contact) {
    boolean primaryExists = isPrimaryExisting(ContactType.METADATA_AUTHOR, target.getContacts());
    contact.setPrimary(!primaryExists);
    // set type to administrative
    contact.setType(ContactType.METADATA_AUTHOR);
    addContact(contact);
  }

  /**
   * Similar to addContact() except that it sets type to ORIGINATOR, and sets isPrimary flag to true
   * only if this is the first contact of type ORIGINATOR in dataset's contact list.
   *
   * @param contact Contact
   */
  public void addPreferredOriginatorContact(Contact contact) {
    boolean primaryExists = isPrimaryExisting(ContactType.ORIGINATOR, target.getContacts());
    contact.setPrimary(!primaryExists);
    // set type to administrative
    contact.setType(ContactType.ORIGINATOR);
    addContact(contact);
  }

  /**
   * Check if primary contact of particular type exists already in list of Contacts.
   *
   * @param contactType type to check for
   * @param contacts list of Contacts
   * @return true if primary contact of particular type exists already, false otherwise
   */
  private boolean isPrimaryExisting(ContactType contactType, List<Contact> contacts) {
    for (Contact c : contacts) {
      if (c.getType() != null && c.getType() == contactType && c.isPrimary()) {
        return true;
      }
    }
    return false;
  }

  public void addTaxonomicCoverages(TaxonomicCoverages taxonomicCoverages) {
    target.getTaxonomicCoverages().add(taxonomicCoverages);
  }

  public void addTemporalCoverage(TemporalCoverage coverage) {
    if (coverage instanceof DateRange) {
      DateRange rangeCoverage = (DateRange) coverage;

      // If the end date is only accurate to the nearest year, set it to the 31 December, for users
      // who don't realize
      // the 0.001s hack.
      if (rangeCoverage.getEnd() != null
          && rangeCoverage.getEnd().toInstant().getNano() == 1_000_000) {
        Year year = Year.from(rangeCoverage.getEnd().toInstant().atZone(ZoneOffset.UTC));
        rangeCoverage.setEnd(
            Date.from(
                year.atDay(year.isLeap() ? 366 : 365)
                    .atStartOfDay(ZoneOffset.UTC)
                    .plusNanos(1_000_000)
                    .toInstant()));
      }
    }

    target.getTemporalCoverages().add(coverage);
  }

  public Dataset getTarget() {
    return target;
  }

  public void setAdditionalInfo(String additionalInfo) {
    target.setAdditionalInfo(additionalInfo);
  }

  public void setBibliographicCitations(List<Citation> bibliographicCitations) {
    target.setBibliographicCitations(bibliographicCitations);
  }

  public void setCitation(Citation citation) {
    target.setCitation(citation);
  }

  public void setContacts(List<Contact> contacts) {
    target.setContacts(contacts);
  }

  public void setCuratorialUnits(List<CuratorialUnitComposite> curatorialUnits) {
    target.setCuratorialUnits(curatorialUnits);
  }

  public void setDataLanguage(Language language) {
    target.setDataLanguage(language);
  }

  public void setLanguage(Language language) {
    target.setLanguage(language);
  }

  public void setDescription(String description) {
    target.setDescription(description);
  }

  public void setGeographicCoverageDescription(String geographicCoverageDescription) {
    target.setGeographicCoverageDescription(geographicCoverageDescription);
  }

  public void setGeographicCoverages(List<GeospatialCoverage> geographicCoverages) {
    target.setGeographicCoverages(geographicCoverages);
  }

  public void setHomepage(URI homepage) throws URISyntaxException {
    target.setHomepage(homepage);
  }

  public void setIdentifiers(List<Identifier> identifiers) {
    target.setIdentifiers(identifiers);
  }

  public void setRights(String rights) {
    target.setRights(rights);
  }

  /**
   * Sets license detected from machine readable license supplied in two parts: URI (ulink@url),
   * title (ulink/citetite). Note only supported and unsupported licenses are set.
   *
   * @param uriString license URI
   * @param title license title
   */
  public void setLicense(@Nullable String uriString, @Nullable String title) {
    LicenseParser licenseParser = LicenseParser.getInstance();

    URI uri = null;
    try {
      uri = StringUtils.isEmpty(uriString) ? null : URI.create(uriString);
    } catch (IllegalArgumentException e) {
      LOG.error(
          "Bad URI found when parsing eml/dataset/intellectualRights/para/ulink@url attribute: {}",
          uriString);
    }

    License license = licenseParser.parseUriThenTitle(uri, title);
    // TODO ensure license not overwritten by UNSPECIFIED and UNSUPPORTED license in
    // datasetService.insertMetadata()

    switch (license) {
      case UNSPECIFIED:
        LOG.debug("No machine readable license was detected!");
        break;
      case UNSUPPORTED:
        // The metadata may provide multiple licenses, in which case we choose one we support.
        if (target.getLicense() == null || target.getLicense().equals(License.UNSPECIFIED)) {
          LOG.debug(
              "An unsupported machine readable license was detected with URI {} and title {}",
              uriString,
              title);
          target.setLicense(license);
        } else {
          LOG.debug(
              "An additional unsupported machine readable license was detected with URI {} and title {}, it will be ignored",
              uriString,
              title);
        }
        break;
      default:
        LOG.debug("A supported machine readable license was detected: {}", license);
        target.setLicense(license);
    }
  }

  public void setCountryCoverage(Set<Country> countryCoverage) {
    target.setCountryCoverage(countryCoverage);
  }

  public void setKey(UUID key) {
    target.setKey(key);
  }

  public void setKeywordCollections(List<KeywordCollection> keywordCollections) {
    target.setKeywordCollections(keywordCollections);
  }

  public void setLogoURL(URI logoURL) {
    target.setLogoUrl(logoURL);
  }

  public void setPublishingOrganizationKey(UUID publishingOrganizationKey) {
    target.setPublishingOrganizationKey(publishingOrganizationKey);
  }

  public void setProject(Project project) {
    target.setProject(project);
  }

  /**
   * Concatenates the new paragrpah to an existing one, inserting a new html break line.
   *
   * @param existing
   * @param para
   */
  private String appendParagraph(String existing, String para) {
    if (!para.isEmpty()) {
      return existing + "<br/>" + para.trim();
    }
    return existing;
  }

  /** Appends paragraph to last MethodStep description. */
  public void appendMethodStepParagraph(String paragraph) {
    final int lastIdx = target.getSamplingDescription().getMethodSteps().size() - 1;
    target
        .getSamplingDescription()
        .getMethodSteps()
        .set(
            lastIdx,
            appendParagraph(
                target.getSamplingDescription().getMethodSteps().get(lastIdx), paragraph));
  }

  /** Adds an empty methodStep to MethodStep List. */
  public void addMethodStep(ParagraphContainer para) {
    if (para != null) {
      target.getSamplingDescription().getMethodSteps().add(para.toString());
    }
  }

  /**
   * Sets the publication date, converting from input String to Date. The beanutils DateTime can't
   * be converted, because this property isn't registered as a proper bean, it is parsed out by the
   * addMethod as a String.
   *
   * @param pubDateAsString publication date as String
   */
  public void setPubDateAsString(String pubDateAsString) {
    try {
      target.setPubDate(calendarDate(pubDateAsString));
    } catch (ParseException e) {
      LOG.error(
          "The publication date was invalid: {}. Expected format is YYYY-MM-DD", pubDateAsString);
    }
  }

  public void setPurpose(String purpose) {
    target.setPurpose(purpose);
  }

  public void setMaintenanceDescription(String maintenanceDescription) {
    target.setMaintenanceDescription(maintenanceDescription);
  }

  public void setMaintenanceUpdateFrequency(MaintenanceUpdateFrequency maintenanceUpdateFrequency) {
    target.setMaintenanceUpdateFrequency(maintenanceUpdateFrequency);
  }

  public void setSamplingDescription(SamplingDescription samplingDescription) {
    target.setSamplingDescription(samplingDescription);
  }

  /**
   * The property should be set against Collection and allow for multiple Collection. There were
   * difficulties doing this because the element is outside the <collection> element so the two
   * properties need to be joined via separate parsing blocks. This is a bit of a hack, assuming
   * there is only ever a single Collection object, and that there is only ever a single
   * SpecimenPreservationMethod that belongs to it. Sets the SpecimenPreservationMethodType on
   * Collection.
   *
   * @param type PreservationMethodType
   */
  public void setSpecimenPreservationMethod(PreservationMethodType type) {
    if (target.getCollections() != null) {
      if (target.getCollections().isEmpty()) {
        target.getCollections().add(new Collection());
      }
      target.getCollections().get(0).setSpecimenPreservationMethod(type);
    }
  }

  public void setPackageId(String id) {
    // is this a DOI?
    if (DOI.isParsable(id)) {
      target.setDoi(new DOI(id));
    } else {
      // check if there is a version
      Matcher m = PACKAGE_ID_VERSION_PATTERN.matcher(id);
      if (m.matches()) {
        target.setVersion(m.group(1));
      }
    }
  }

  public void setSubtype(DatasetSubtype subtype) {
    target.setSubtype(subtype);
  }

  public void setTaxonomicCoverages(List<TaxonomicCoverages> taxonomicCoverages) {
    target.setTaxonomicCoverages(taxonomicCoverages);
  }

  public void setTechnicalInstallationKey(UUID technicalInstallationKey) {
    target.setInstallationKey(technicalInstallationKey);
  }

  public void setTemporalCoverages(List<TemporalCoverage> temporalCoverages) {
    target.setTemporalCoverages(temporalCoverages);
  }

  public void setTitle(String title) {
    // keep first true title in case we encounter several - just to be safe with this important
    // property

    target.setTitle(target.getTitle() != null ? target.getTitle() : Objects.requireNonNull(title));
  }

  public void setType(DatasetType type) {
    target.setType(type);
  }

  public void postProcess() {
    updateTaxonomicCoverageRanks();
    updatePrimaryDOI();

    if (target.getCitation() != null) {
      CleanUtils.removeEmptyStrings(target.getCitation());
    }
  }

  private void updateTaxonomicCoverageRanks() {
    for (TaxonomicCoverages tc : target.getTaxonomicCoverages()) {
      for (TaxonomicCoverage t : tc.getCoverages()) {
        if (t.getRank() != null) {
          t.getRank().setInterpreted(toRank(t.getRank().getVerbatim()));
        }
      }
    }
  }

  /**
   * This extracts the first DOI from alternate identifier or gbif citation identifier if no target
   * DOI existed, e.g. found in the packageID rule before or from the initial target instance.
   */
  private void updatePrimaryDOI() {
    if (target.getDoi() == null) {
      Iterator<Identifier> iter = target.getIdentifiers().iterator();
      while (iter.hasNext()) {
        Identifier i = iter.next();
        if (i.getType() == IdentifierType.DOI) {
          if (DOI.isParsable(i.getIdentifier())) {
            target.setDoi(new DOI(i.getIdentifier()));
            iter.remove();
            return;
          }
        }
      }
      // at last also check the citation field
      if (StringUtils.isNotEmpty(target.getCitation().getIdentifier())) {
        try {
          target.setDoi(new DOI(target.getCitation().getIdentifier()));
        } catch (IllegalArgumentException e) {
          // invalid DOI, skip
        }
      }
    }
  }

  private Rank toRank(String rank) {
    ParseResult<Rank> result = RankParser.getInstance().parse(rank);
    return result.getStatus() == ParseResult.STATUS.SUCCESS ? result.getPayload() : null;
  }
}
