package org.gbif.registry.metadata.parse;

import org.gbif.api.model.common.InterpretedEnum;
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
import org.gbif.api.model.registry.eml.temporal.TemporalCoverage;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.PreservationMethodType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.common.parsers.ParseResult;
import org.gbif.common.parsers.rank.InterpretedRankParser;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A delegating wrapper to a Dataset that can be instructed to override existing content or not.
 * This allows an existing Dataset to be augmented by new content.
 */
public class DatasetWrapper {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetWrapper.class);
  private final Dataset target = new Dataset();

  /**
   * Utility to parse an EML calendarDate in a textual format. Can be ISO date or just the year, ignoring whitespace
   *
   * @param dateString To set in format YYYY-MM-DD
   *
   * @return the parsed date
   *
   * @throws java.text.ParseException Should it be an erroneous format
   * @see <a href="http://knb.ecoinformatics.org/software/eml/eml-2.1.0/eml-coverage.html#calendarDate">EML Coverage
   *      calendarDate keyword</a>
   */
  private static Date calendarDate(String dateString) throws ParseException {
    if (Strings.isNullOrEmpty(dateString)) {
      return null;
    }
    // kill whitespace
    dateString = dateString.replaceAll("\\s", "");
    dateString = dateString.replaceAll("[\\,._#//]", "-");
    Date date;
    try {
      SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd");
      date = iso.parse(dateString);
    } catch (ParseException e) {
      if (dateString.length() == 4) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        date = sdf.parse(dateString);
        date = new Date(date.getTime() + 1);
      } else {
        throw e;
      }
    }
    return date;
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
    target.getContacts().add(contact);
  }

  /**
   * We only ever expect a single set of curatorial units per Collection. The Curatorial Units are outside the
   * Collection element in the GBIF EML schema, so it's by strong assumption that the units belong to the collection.
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

  /**
   * Adds a comma or semicolon concatenated keyword string as keyword collection.
   */
  public void addSubjects(String subjects) {
    Splitter keywordSplitter = Splitter.on(Pattern.compile("[,;]")).trimResults().omitEmptyStrings();
    if (!Strings.isNullOrEmpty(subjects)) {
      KeywordCollection collection = new KeywordCollection();
      for (String keyword : keywordSplitter.split(subjects)) {
        collection.addKeyword(keyword);
      }
      target.getKeywordCollections().add(collection);
    }
  }

  public void addCreator(String creator) {
    if (!Strings.isNullOrEmpty(creator)) {
      Contact contact = new Contact();
      contact.setLastName(creator);
      contact.setType(ContactType.ORIGINATOR);
      target.getContacts().add(contact);
    }
  }

  public void addBibCitation(String citation) {
    if (!Strings.isNullOrEmpty(citation)) {
      Citation c = new Citation();
      c.setText(citation);
      target.setCitation(c);
    }
  }

  public void addIdentifier(String id) {
    if (!Strings.isNullOrEmpty(id)) {
      Identifier i = new Identifier();
      i.setIdentifier(id);
      i.setType(IdentifierType.UNKNOWN);
      target.getIdentifiers().add(i);
    }
  }

  public void addAbstract(String text) {
    if (!Strings.isNullOrEmpty(text) && Strings.isNullOrEmpty(target.getDescription())) {
      target.setDescription(text);
    }
  }

  public void addLicense(String license) {
    if (!Strings.isNullOrEmpty(license)) {
      if (Strings.isNullOrEmpty(target.getRights())) {
        target.setRights(license);
      } else {
        target.setRights(target.getRights() + " \n" + license);
      }
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
   * Similar to addContact() except that it is always used to set preferred flag to true on Contact, and type to
   * ADMINISTRATIVE_POINT_OF_CONTACT.
   *
   * @param contact Contact
   */
  public void addPreferredAdministrativeContact(Contact contact) {
    contact.setPrimary(true);
    // set type to administrative
    contact.setType(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT);
    target.getContacts().add(contact);
  }

  /**
   * Similar to addContact() except that it is always used to set preferred flag to true on Contact, and type to
   * METADATA_AUTHOR.
   *
   * @param contact Contact
   */
  public void addPreferredMetadataContact(Contact contact) {
    // set preferred = true
    contact.setPrimary(true);
    // set type to administrative
    contact.setType(ContactType.METADATA_AUTHOR);
    target.getContacts().add(contact);
  }

  /**
   * Similar to addContact() except that it is always used to set preferred flag to true on Contact, and type to
   * ORIGINATOR.
   *
   * @param contact Contjoact
   */
  public void addPreferredOriginatorContact(Contact contact) {
    // set preferred = true
    contact.setPrimary(true);
    // set type to administrative
    contact.setType(ContactType.ORIGINATOR);
    target.getContacts().add(contact);
  }

  public void addTaxonomicCoverages(TaxonomicCoverages taxonomicCoverages) {
    target.getTaxonomicCoverages().add(taxonomicCoverages);
  }

  public void addTemporalCoverage(TemporalCoverage coverage) {
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

  public void setOwningOrganizationKey(UUID owningOrganizationKey) {
    target.setOwningOrganizationKey(owningOrganizationKey);
  }

  public void setProject(Project project) {
    target.setProject(project);
  }


  /**
   * Concatenates the new paragrpah to an existing one, inserting a new html break line.
   * @param existing
   * @param para
   */
  private String appendParagraph(String existing, String para) {
    if (!para.isEmpty()) {
      return existing + "<br/>" + para.trim();
    }
    return existing;
  }

  /**
   * Appends paragraph to last MethodStep description.
   */
  public void appendMethodStepParagraph(String paragraph) {
    final int lastIdx = target.getSamplingDescription().getMethodSteps().size() - 1;
    target.getSamplingDescription().getMethodSteps().set(lastIdx,
      appendParagraph(target.getSamplingDescription().getMethodSteps().get(lastIdx), paragraph));
  }

  /**
   * Adds an empty methodStep to MethodStep List.
   */
  public void addMethodStep(ParagraphContainer para) {
    if (para != null) {
      target.getSamplingDescription().getMethodSteps().add(para.toString());
    }
  }

  /**
   * Sets the publication date, converting from input String to Date. The beanutils DateTime can't be converted,
   * because this property isn't registered as a proper bean, it is parsed out by the addMethod as a String.
   *
   * @param pubDateAsString publication date as String
   */
  public void setPubDateAsString(String pubDateAsString) {
    try {
      target.setPubDate(calendarDate(pubDateAsString));
    } catch (ParseException e) {
      LOG.error("The publication date was invalid: {}. Expected format is YYYY-MM-DD", pubDateAsString);
    }
  }

  public void setPurpose(String purpose) {
    target.setPurpose(purpose);
  }

  public void setSamplingDescription(SamplingDescription samplingDescription) {
    target.setSamplingDescription(samplingDescription);
  }

  /**
   * The property should be set against Collection and allow for multiple Collection. There were difficulties
   * doing this because the element is outside the <collection> element so the two properties need to be joined via
   * separate parsing blocks.
   * This is a bit of a hack, assuming there is only ever a single Collection object, and that there is only ever a
   * single SpecimenPreservationMethod that belongs to it.
   * Sets the SpecimenPreservationMethodType on Collection.
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
    // keep first true title in case we encounter several - just to be safe with this important property
    target.setTitle(Objects.firstNonNull(target.getTitle(), title));
  }

  public void setType(DatasetType type) {
    target.setType(type);
  }

  public void updateTaxonomicCoverageRanks() {
    for (TaxonomicCoverages tc : target.getTaxonomicCoverages()) {
      for (TaxonomicCoverage t : tc.getCoverages()) {
        if (t.getRank() != null) {
          t.getRank().setInterpreted(toRank(t.getRank().getVerbatim()));
        }
      }
    }
  }

  private Rank toRank(String rank) {
    ParseResult<InterpretedEnum<String, Rank>> result = InterpretedRankParser.getInstance().parse(rank);
    return result.getStatus() == ParseResult.STATUS.SUCCESS ? result.getPayload().getInterpreted() : null;
  }
}
