package org.gbif.registry.doi.converter;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.model.registry.eml.geospatial.BoundingBox;
import org.gbif.api.model.registry.eml.geospatial.GeospatialCoverage;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.NameIdentifier;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.registry.doi.converter.DatasetConverter;
import org.junit.Test;
import org.xmlunit.matchers.CompareMatcher;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class DatasetConverterTest {

  @Test
  public void testConvertDataset() throws Exception {
    Organization publisher = new Organization();
    publisher.setTitle("X-Publisher");
    publisher.setKey(UUID.randomUUID());

    final DOI doi = new DOI("10.1234/21373");
    Dataset d = new Dataset();
    d.setKey(UUID.fromString("4b3c4936-24fc-4cbd-886d-3f874a44e31f"));
    d.setType(DatasetType.OCCURRENCE);
    d.setTitle("my title");
    d.setCreated(new Date(1569967363000L));
    d.setModified(new Date(1570053763000L));
    d.setCreatedBy("Markus GBIF User");
    d.setLanguage(Language.NORWEGIAN);
    d.setDataLanguage(Language.NORWEGIAN);
    d.setLicense(License.CC0_1_0);

    KeywordCollection kc1 = new KeywordCollection();
    Set<String> keywords1 = new HashSet<>();
    keywords1.add("Specimen");
    kc1.setThesaurus("GBIF Dataset Subtype Vocabulary");
    kc1.setKeywords(keywords1);
    d.getKeywordCollections().add(kc1);

    KeywordCollection kc2 = new KeywordCollection();
    Set<String> keywords2 = new HashSet<>();
    keywords2.add("Occurrence");
    keywords2.add("Human observation");
    kc2.setThesaurus("GBIF Dataset Subtype Vocabulary");
    kc2.setKeywords(keywords2);
    d.getKeywordCollections().add(kc2);

    Contact contact1 = new Contact();
    contact1.setFirstName("Markus");
    contact1.setType(ContactType.ORIGINATOR);
    contact1.setOrganization("GBIF");
    // TODO: 02/10/2019 try more than one
    contact1.setUserId(Collections.singletonList("https://orcid.org/0000-0000-0000-0001"));
    d.getContacts().add(contact1);

    Contact contact11 = new Contact();
    contact11.setFirstName("John");
    contact11.setType(ContactType.ORIGINATOR);
    contact11.setOrganization("GBIF");
    d.getContacts().add(contact11);

    Contact contact2 = new Contact();
    contact2.setFirstName("Hubert");
    contact2.setLastName("Reeves");
    contact2.setType(ContactType.METADATA_AUTHOR);
    contact2.setOrganization("DataCite");
    contact2.setUserId(Collections.singletonList("http://orcid.org/0000-0000-0000-0002"));
    d.getContacts().add(contact2);

    Contact contact22 = new Contact();
    contact22.setFirstName("Joe");
    contact22.setLastName("Smith");
    contact22.setOrganization("DataCite");
    contact22.setType(ContactType.METADATA_AUTHOR);
    d.getContacts().add(contact22);

    // add an author with no name
    Contact contact3 = new Contact();
    contact3.setType(ContactType.ORIGINATOR);
    contact3.setOrganization("GBIF");
    d.getContacts().add(contact3);

    // add a contributor with no name
    Contact contact4 = new Contact();
    contact4.setType(ContactType.METADATA_AUTHOR);
    contact4.setOrganization("DataCite");
    d.getContacts().add(contact4);

    d.setDoi(doi);
    d.setDescription("some description");
    List<GeospatialCoverage> geos = Lists.newArrayList();
    d.setGeographicCoverages(geos);
    GeospatialCoverage g1 = new GeospatialCoverage();
    geos.add(g1);
    g1.setDescription("geo description");
    g1.setBoundingBox(new BoundingBox(1, 2, 3, 4));
    GeospatialCoverage g2 = new GeospatialCoverage();
    geos.add(g2);
    g2.setDescription("geo description 2");
    g2.setBoundingBox(new BoundingBox(5, 6, 7, 8));

//    final DataCiteMetadata expectedMetadata = DataCiteValidator.fromXml(FileUtils.classpathStream("metadata/metadata-dataset.xml"));
//    final String expected = DataCiteValidator.toXml(expectedMetadata, true);
    String expected = Resources.toString(Resources.getResource("metadata/metadata-dataset.xml"), Charsets.UTF_8);

    final String actual = convertToXml(doi, d, publisher);

    assertEquals(expected, actual);
    assertThat(actual, CompareMatcher.isIdenticalTo(expected).ignoreWhitespace().normalizeWhitespace());
  }

  private DataCiteMetadata convertAndValidate(DOI doi, Dataset d, Organization publisher) throws InvalidMetadataException {
    DataCiteMetadata m = DatasetConverter.convert(d, publisher);
    DataCiteValidator.toXml(doi, m);
    return m;
  }

  private String convertToXml(DOI doi, Dataset d, Organization publisher) throws InvalidMetadataException {
    DataCiteMetadata m = DatasetConverter.convert(d, publisher);
    return DataCiteValidator.toXml(doi, m);
  }

  @Test
  public void testDatasetLicense() throws Exception {
    Organization publisher = new Organization();
    publisher.setTitle("My Publisher");
    publisher.setKey(UUID.randomUUID());

    final DOI doi = new DOI("10.1234/5679");
    Dataset d = new Dataset();
    d.setKey(UUID.randomUUID());
    d.setType(DatasetType.METADATA);
    d.setTitle("My Metadata");
    d.setCreated(new Date());
    d.setModified(new Date());
    d.setCreatedBy("Jim");
    d.setLanguage(Language.ENGLISH);
    d.setDataLanguage(Language.ENGLISH);
    d.setRights("Copyright ©");

    DataCiteMetadata m = convertAndValidate(doi, d, publisher);
    assertEquals("Copyright ©", m.getRightsList().getRights().get(0).getValue());
  }

  @Test
  public void testUserIdToNameIdentifier() {
    NameIdentifier creatorNid =
      DatasetConverter.userIdToNameIdentifier(Collections.singletonList("http://orcid.org/0000-0000-0000-0001"));
    assertEquals("http://orcid.org/", creatorNid.getSchemeURI());
    assertEquals("0000-0000-0000-0001", creatorNid.getValue());
  }

  @Test
  public void testGetYear() {
    Date d = new Date(1418340702253L);
    assertEquals("2014", DatasetConverter.getYear(d));
    assertNull(DatasetConverter.getYear(null));
  }
}
