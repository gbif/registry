package org.gbif.registry.doi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadFormat;
import org.gbif.api.model.occurrence.PredicateDownloadRequest;
import org.gbif.api.model.occurrence.predicate.EqualsPredicate;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
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
import org.gbif.occurrence.query.TitleLookup;
import org.junit.Test;
import com.beust.jcommander.internal.Lists;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.xmlunit.matchers.CompareMatcher;

public class DataCiteConverterTest {

  @Test
  public void testGetYear() throws Exception {
    Date d = new Date(1418340702253L);
    assertEquals("2014", DataCiteConverter.getYear(d));
    assertNull(DataCiteConverter.getYear(null));
  }
  // TODO: 02/10/2019 check coverage, test all the cases

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

    DataCiteMetadata m = convertAndValidate(doi, d, publisher);
    assertEquals("my title", m.getTitles().getTitle().get(0).getValue());
    assertEquals("Markus", m.getCreators().getCreator().get(0).getCreatorName().getValue());
    assertEquals(doi.getDoiName(), m.getIdentifier().getValue());

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

    m = convertAndValidate(doi, d, publisher);
    assertEquals("my title", m.getTitles().getTitle().get(0).getValue());
    assertEquals("Markus", m.getCreators().getCreator().get(0).getCreatorName().getValue());
    assertEquals("Hubert Reeves", m.getContributors().getContributor().get(0).getContributorName().getValue());
    assertEquals("10.1234/5678", m.getIdentifier().getValue());
    assertEquals(Lists.<Double>newArrayList(1d, 3d, 2d, 4d), m.getGeoLocations().getGeoLocation().get(0).getGeoLocationPlaceOrGeoLocationPointOrGeoLocationBox());
    assertEquals(d.getDescription(), m.getDescriptions().getDescription().get(0).getContent().get(0));
    assertEquals(License.CC0_1_0.getLicenseUrl(), m.getRightsList().getRights().get(0).getRightsURI());

    // -2 is to subtract those with no name (they should be ignored)
    assertEquals(d.getContacts().size() -2, m.getCreators().getCreator().size() +
            m.getContributors().getContributor().size());

    assertNotNull("RelatedIdentifiers is expected to be not null", m.getRelatedIdentifiers());
  }

  private DataCiteMetadata convertAndValidate(DOI doi, Dataset d, Organization publisher) throws InvalidMetadataException {
    DataCiteMetadata m = DataCiteConverter.convert(d, publisher);
    DataCiteValidator.toXml(doi, m);
    return m;
  }

  private String convertToXml(DOI doi, Dataset d, Organization publisher) throws InvalidMetadataException {
    DataCiteMetadata m = DataCiteConverter.convert(d, publisher);
    return DataCiteValidator.toXml(doi, m);
  }

  @Test
  public void testConvertDownload() throws Exception {
    DatasetOccurrenceDownloadUsage du1 = new DatasetOccurrenceDownloadUsage();
    du1.setDatasetKey(UUID.randomUUID());
    du1.setDatasetTitle("my title");
    du1.setDatasetDOI(new DOI("10.1234/5679"));
    du1.setNumberRecords(101);

    DatasetOccurrenceDownloadUsage du2 = new DatasetOccurrenceDownloadUsage();
    du2.setDatasetKey(UUID.randomUUID());
    du2.setDatasetTitle("my title #2");
    du2.setDatasetDOI(new DOI("10.1234/klimbim"));
    du2.setNumberRecords(2002);

    Download download = new Download();
    download.setCreated(new Date());
    download.setDoi(new DOI("10.1234/5678"));
    download.setKey("1");
    download.setModified(new Date());
    download.setNumberDatasets(1L);
    download.setSize(100);
    download.setStatus(Download.Status.SUCCEEDED);
    download.setTotalRecords(10);
    PredicateDownloadRequest downloadRequest = new PredicateDownloadRequest();
    downloadRequest.setCreator("dev@gbif.org");
    downloadRequest.setPredicate(new EqualsPredicate(OccurrenceSearchParameter.TAXON_KEY, "3"));
    downloadRequest.setFormat(DownloadFormat.DWCA);
    download.setRequest(downloadRequest);

    GbifUser user = new GbifUser();
    user.setUserName("occdownload.gbif.org");
    user.setEmail("occdownload@devlist.gbif.org");
    user.setFirstName(null);
    user.setLastName("GBIF.org");

    // mock title lookup API
    TitleLookup tl = mock(TitleLookup.class);
    when(tl.getDatasetTitle(anyString())).thenReturn("PonTaurus");
    when(tl.getSpeciesName(anyString())).thenReturn("Abies alba Mill.");

    DataCiteMetadata metadata = DataCiteConverter.convert(download, user, Lists.newArrayList(du1, du2), tl);
    String xml = DataCiteValidator.toXml(download.getDoi(), metadata);
    assertTrue(xml.contains(du1.getDatasetDOI().getDoiName()));
    assertTrue(xml.contains(du2.getDatasetDOI().getDoiName()));
    assertTrue(xml.contains(String.valueOf(du1.getNumberRecords())));
    assertTrue(xml.contains(String.valueOf(du2.getNumberRecords())));
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
            DataCiteConverter.userIdToNameIdentifier(Collections.singletonList("http://orcid.org/0000-0000-0000-0001"));
    assertEquals("http://orcid.org/", creatorNid.getSchemeURI());
    assertEquals("0000-0000-0000-0001", creatorNid.getValue());
  }

  @Test
  public void testTruncateDesription() throws Exception {
    DOI doi = new DOI("10.15468/dl.v8zc57");
    String xml = Resources.toString(Resources.getResource("metadata/datacite-large.xml"), Charsets.UTF_8);
    String xml2 = DataCiteConverter.truncateDescription(doi, xml, URI.create("http://gbif.org"));
    //System.out.println(xml2);
    DataCiteValidator.validateMetadata(xml2);
    assertTrue(xml2.contains("for full list of all constituents"));
    assertFalse(xml2.contains("University of Ghent"));
    assertTrue(xml2.contains("10.15468/siye1z"));
    assertEquals(3690, xml2.length());
  }

  @Test
  public void testTruncateConstituents() throws Exception {
    DOI doi = new DOI("10.15468/dl.v8zc57");
    String xml = Resources.toString(Resources.getResource("metadata/datacite-large.xml"), Charsets.UTF_8);
    String xml2 = DataCiteConverter.truncateConstituents(doi, xml, URI.create("http://gbif.org"));
    DataCiteValidator.validateMetadata(xml2);
    assertTrue(xml2.contains("for full list of all constituents"));
    assertFalse(xml2.contains("University of Ghent"));
    assertFalse(xml2.contains("10.15468/siye1z"));
    assertEquals(2352, xml2.length());
  }
}
