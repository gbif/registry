package org.gbif.registry.metadata.parse;

import org.apache.commons.io.input.ReaderInputStream;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.eml.Collection;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.model.registry.eml.temporal.DateRange;
import org.gbif.api.model.registry.eml.temporal.SingleDate;
import org.gbif.api.model.registry.eml.temporal.VerbatimTimePeriod;
import org.gbif.api.model.registry.eml.temporal.VerbatimTimePeriodType;
import org.gbif.api.vocabulary.*;
import org.gbif.registry.metadata.EMLWriter;
import org.gbif.utils.file.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.*;

public class DatasetParserTest {


  @Test
  public void testDetectParserType() throws Exception {
    MetadataType type = DatasetParser.detectParserType(FileUtils.classpathStream("dc/worms_dc.xml"));
    assertEquals(MetadataType.DC, type);

    type = DatasetParser.detectParserType(FileUtils.classpathStream("eml/ipt_eml.xml"));
    assertEquals(MetadataType.EML, type);

    type = DatasetParser.detectParserType(FileUtils.classpathStream("eml/clb_eml.xml"));
    assertEquals(MetadataType.EML, type);

    type = DatasetParser.detectParserType(FileUtils.classpathStream("eml/sample.xml"));
    assertEquals(MetadataType.EML, type);

    type = DatasetParser.detectParserType(FileUtils.classpathStream("eml/eml_utf8_bom.xml"));
    assertEquals(MetadataType.EML, type);

    type = DatasetParser.detectParserType(FileUtils.classpathStream("eml/sample-breaking.xml"));
    assertEquals(MetadataType.EML, type);

    type = DatasetParser.detectParserType(FileUtils.classpathStream("eml/eml-protocol.xml"));
    assertNull(type);

    type = DatasetParser.detectParserType(FileUtils.classpathStream("logback-test.xml"));
    assertNull(type);

    type = DatasetParser.detectParserType(FileUtils.classpathStream("dc/dc_broken.xml"));
    assertNull(type);
  }



  @Test(expected = IllegalArgumentException.class)
  public void testBuild() throws Exception {
    Dataset d = DatasetParser.build(FileUtils.classpathStream("dc/worms_dc.xml"));
    assertNotNull(d);

    d = DatasetParser.build(FileUtils.classpathStream("eml/sample.xml"));
    assertNotNull(d);

    d = DatasetParser.build(FileUtils.classpathStream("eml/ipt_eml.xml"));
    assertNotNull(d);

    DatasetParser.build(FileUtils.classpathStream("logback-test.xml"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildProtocol() throws Exception {
    Dataset d = DatasetParser.build(FileUtils.classpathStream("eml/eml-protocol.xml"));
    d.getTitle();
  }

  private Contact contactByType(Dataset d, ContactType type) {
    for (Contact c : d.getContacts()) {
      if (type == c.getType()) {
        return c;
      }
    }
    return null;
  }

  private void assertIdentifierExists(Dataset d, String id, IdentifierType type) {
    for (Identifier i : d.getIdentifiers()) {
      if (i.getType() == type && id.equals(i.getIdentifier())) {
        return;
      }
    }
    fail("Identifier " + id + " of type " + type + " missing");
  }

  private void assertKeywordExists(Dataset d, String tag) {
    for (KeywordCollection kc : d.getKeywordCollections()) {
      for (String k : kc.getKeywords()) {
        if (k.equals(tag)) {
          return;
        }
      }
    }
    fail("Keyword" + tag+ " missing");
  }

  @Test
  public void testDcParsing() throws Exception {
    Dataset dataset = DatasetParser.parse(MetadataType.DC, FileUtils.classpathStream("dc/worms_dc.xml"));

    Calendar cal = Calendar.getInstance();
    cal.clear();

    assertNotNull(dataset);

    assertEquals("World Register of Marine Species", dataset.getTitle());
    assertTrue(dataset.getDescription().startsWith(
      "The aim of a World Register of Marine Species (WoRMS) is to provide an authoritative and comprehensive list of names of marine organisms, including information on synonymy. While highest priority goes to valid names, other names in use are included so that this register can serve as a guide to interpret taxonomic literature."));
    assertEquals("http://www.marinespecies.org/", dataset.getHomepage().toString());
    assertEquals("Ward Appeltans", contactByType(dataset, ContactType.ORIGINATOR).getLastName());
    assertEquals("World Register of Marine Species", dataset.getTitle());
    assertEquals("World Register of Marine Species", dataset.getTitle());
    assertEquals("World Register of Marine Species", dataset.getTitle());

    assertIdentifierExists(dataset, "1234", IdentifierType.UNKNOWN);
    assertIdentifierExists(dataset, "doi:10.1093/ageing/29.1.57", IdentifierType.UNKNOWN);
    assertIdentifierExists(dataset, "http://ageing.oxfordjournals.org/content/29/1/57", IdentifierType.UNKNOWN);

    assertKeywordExists(dataset, "Specimens");
    assertKeywordExists(dataset, "Authoritative");
    assertKeywordExists(dataset, "Species Checklist");
    assertKeywordExists(dataset, "Taxonomy");
    assertKeywordExists(dataset, "Marine");
  }


  @Test
  public void testEmlParsing() {
    try {
        Dataset dataset = DatasetParser.parse(MetadataType.EML, FileUtils.classpathStream("eml/sample.xml"));
        verifySample(dataset);

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }


    @Test
    public void testRoundtripping() {
        try {
            Dataset dataset = DatasetParser.parse(MetadataType.EML, FileUtils.classpathStream("eml/sample.xml"));
            verifySample(dataset);

            // write again to eml file and read again
            StringWriter writer = new StringWriter();
            EMLWriter.write(dataset, writer);

            final String eml = writer.toString();
            System.out.println(eml);
            InputStream in = new ReaderInputStream(new StringReader(eml), Charset.forName("UTF8"));
            Dataset dataset2 = DatasetParser.parse(MetadataType.EML, in);
            verifySample(dataset2);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void verifySample(Dataset dataset) {
        Calendar cal = Calendar.getInstance();
        cal.clear();

        assertNotNull(dataset);

        assertIdentifierExists(dataset, "619a4b95-1a82-4006-be6a-7dbe3c9b33c5", IdentifierType.UUID);
        assertIdentifierExists(dataset, "doi:10.1093/ageing/29.1.57", IdentifierType.DOI);
        assertIdentifierExists(dataset, "http://ageing.oxfordjournals.org/content/29/1/57", IdentifierType.URL);

        // assertEquals(7, dataset.getEmlVersion());
        assertEquals("Tanzanian Entomological Collection", dataset.getTitle());

        // this is complete test for agents so subsequent agent tests will not be
        // so extensive
        List<Contact> contactList = dataset.getContacts();
        assertEquals(5, contactList.size());

        // test Creator
        Contact contact = contactList.get(0);
        assertEquals("DavidTheCreator", contact.getFirstName());
        assertEquals("Remsen", contact.getLastName());
        assertEquals("ECAT Programme Officer", contact.getPosition().get(0));
        assertEquals("GBIF", contact.getOrganization());
        assertEquals("Universitestparken 15", contact.getAddress().get(0));
        assertEquals("Copenhagen", contact.getCity());
        assertEquals("Sjaelland", contact.getProvince());
        assertEquals("2100", contact.getPostalCode());
        assertEquals(Country.DENMARK, contact.getCountry());
        assertEquals("+4528261487", contact.getPhone().get(0));
        assertEquals("dremsen@gbif.org", contact.getEmail().get(0));
        assertTrue(contact.isPrimary());
        assertEquals(ContactType.ORIGINATOR, contact.getType());

        // test Metadata provider
        contact = contactList.get(1);
        assertEquals("Tim", contact.getFirstName());
        assertEquals("Robertson", contact.getLastName());
        assertEquals("Universitestparken 15", contact.getAddress().get(0));
        assertEquals("Copenhagen", contact.getCity());
        assertEquals("Copenhagen", contact.getProvince());
        assertEquals("2100", contact.getPostalCode());
        assertEquals(Country.DENMARK, contact.getCountry());
        assertEquals("+4528261487", contact.getPhone().get(0));
        assertEquals("trobertson@gbif.org", contact.getEmail().get(0));
        assertTrue(contact.isPrimary());
        assertEquals(ContactType.METADATA_AUTHOR, contact.getType());
        assertEquals("http://orcid.org/0000-0001-5337-1153", contact.getUserId().get(0));
        assertEquals("gbif:153", contact.getUserId().get(1));

        // test Contact
        contact = contactList.get(4);
        assertEquals("David", contact.getFirstName());
        assertEquals("Remsen", contact.getLastName());
        assertEquals("ECAT Programme Officer", contact.getPosition().get(0));
        assertEquals("GBIF", contact.getOrganization());
        assertEquals("Universitestparken 15", contact.getAddress().get(0));
        assertEquals("Copenhagen", contact.getCity());
        assertEquals("Sjaelland", contact.getProvince());
        assertEquals("2100", contact.getPostalCode());
        assertEquals(Country.DENMARK, contact.getCountry());
        assertEquals("+4528261487", contact.getPhone().get(0));
        assertEquals("dremsen@gbif.org", contact.getEmail().get(0));
        assertTrue(contact.isPrimary());
        assertEquals(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT, contact.getType());

        // test limited agent with role tests
        contact = contactList.get(2);
        assertEquals(ContactType.PRINCIPAL_INVESTIGATOR, contact.getType());
        assertFalse(contact.isPrimary());
        assertEquals("Markus", contact.getFirstName());
        assertEquals("Döring", contact.getLastName());
        assertEquals("2nd floor", contact.getAddress().get(0));
        assertEquals("Universitestparken 15", contact.getAddress().get(1));
        assertEquals("Copenhagen", contact.getCity());
        assertEquals("Copenhagen", contact.getProvince());
        assertEquals("2100", contact.getPostalCode());
        assertEquals(Country.DENMARK, contact.getCountry());
        assertEquals("+45 35321 487", contact.getPhone().get(0));
        assertEquals("+45 35321 488", contact.getPhone().get(1));
        assertEquals("mdoering@gbif.org", contact.getEmail().get(0));
        assertEquals("mdoering@mailinator.com", contact.getEmail().get(1));
        assertEquals("http://www.gbif.org", contact.getHomepage().get(0).toString());
        assertEquals("http://www.gbif-dev.org", contact.getHomepage().get(1).toString());



        contact = contactList.get(3);
        // assertEquals("pointOfContact", contact.getRole());
        assertEquals(ContactType.POINT_OF_CONTACT, contact.getType());
        assertFalse(contact.isPrimary());

        // Publication Date
        cal.clear();
        cal.set(2010, Calendar.FEBRUARY, 2);
        assertEquals(cal.getTime(), dataset.getPubDate());

        // WritableDataset properties
        // xml:lang="en"
        assertEquals(Language.ENGLISH, dataset.getLanguage());
        // <language>en</language>
        assertEquals(Language.ENGLISH, dataset.getDataLanguage());
        assertEquals("Specimens in jars.\nCollected over years.\nStill being curated.", dataset.getDescription());
        assertEquals(buildURI("http://www.any.org/fauna/coleoptera/beetleList.html"), dataset.getHomepage());
        assertEquals(buildURI("http://www.tim.org/logo.jpg"), dataset.getLogoUrl());

        // KeywordSets
        assertNotNull(dataset.getKeywordCollections());
        assertEquals(2, dataset.getKeywordCollections().size());
        assertNotNull(dataset.getKeywordCollections().get(0).getKeywords());
        assertEquals(3, dataset.getKeywordCollections().get(0).getKeywords().size());
        assertTrue(dataset.getKeywordCollections().get(0).getKeywords().contains("Insect"));
        assertTrue(dataset.getKeywordCollections().get(0).getKeywords().contains("Insect"));
        assertTrue(dataset.getKeywordCollections().get(0).getKeywords().contains("Insect"));
        assertEquals("Zoology Vocabulary Version 1", dataset.getKeywordCollections().get(0).getThesaurus());
        assertEquals(1, dataset.getKeywordCollections().get(1).getKeywords().size());
        assertTrue(dataset.getKeywordCollections().get(1).getKeywords().contains("Spider"));
        assertEquals("Zoology Vocabulary Version 1", dataset.getKeywordCollections().get(1).getThesaurus());

        // geospatial coverages tests
        assertNotNull(dataset.getGeographicCoverages());
        assertEquals(2, dataset.getGeographicCoverages().size());
        assertEquals("Bounding Box 1", dataset.getGeographicCoverages().get(0).getDescription());
        assertEquals("23.975", String.valueOf(dataset.getGeographicCoverages().get(0).getBoundingBox().getMaxLatitude()));
        assertEquals("0.703", String.valueOf(dataset.getGeographicCoverages().get(0).getBoundingBox().getMaxLongitude()));
        assertEquals("-22.745", String.valueOf(dataset.getGeographicCoverages().get(0).getBoundingBox().getMinLatitude()));
        assertEquals("-1.564", String.valueOf(dataset.getGeographicCoverages().get(0).getBoundingBox().getMinLongitude()));
        assertEquals("Bounding Box 2", dataset.getGeographicCoverages().get(1).getDescription());
        assertEquals("43.975", String.valueOf(dataset.getGeographicCoverages().get(1).getBoundingBox().getMaxLatitude()));
        assertEquals("11.564", String.valueOf(dataset.getGeographicCoverages().get(1).getBoundingBox().getMaxLongitude()));
        assertEquals("-32.745", String.valueOf(dataset.getGeographicCoverages().get(1).getBoundingBox().getMinLatitude()));
        assertEquals("-10.703", String.valueOf(dataset.getGeographicCoverages().get(1).getBoundingBox().getMinLongitude()));

        // temporal coverages tests
        assertEquals(4, dataset.getTemporalCoverages().size());
        cal.clear();
        cal.set(2009, Calendar.DECEMBER, 1);
        assertEquals(DateRange.class, dataset.getTemporalCoverages().get(0).getClass());
        DateRange d = (DateRange) dataset.getTemporalCoverages().get(0);
        assertEquals(cal.getTime(), d.getStart());
        cal.set(2009, Calendar.DECEMBER, 30);
        assertEquals(cal.getTime(), d.getEnd());

        assertEquals(SingleDate.class, dataset.getTemporalCoverages().get(1).getClass());
        cal.set(2008, Calendar.JUNE, 1);
        SingleDate d2 = (SingleDate) dataset.getTemporalCoverages().get(1);
        assertEquals(cal.getTime(), d2.getDate());

        assertEquals(VerbatimTimePeriod.class, dataset.getTemporalCoverages().get(2).getClass());
        VerbatimTimePeriod d3 = (VerbatimTimePeriod) dataset.getTemporalCoverages().get(2);
        assertEquals("During the 70s", d3.getPeriod());
        assertEquals(VerbatimTimePeriodType.FORMATION_PERIOD, d3.getType());

        assertEquals(VerbatimTimePeriod.class, dataset.getTemporalCoverages().get(3).getClass());
        d3 = (VerbatimTimePeriod) dataset.getTemporalCoverages().get(3);
        assertEquals("Jurassic", d3.getPeriod());
        assertEquals(VerbatimTimePeriodType.LIVING_TIME_PERIOD, d3.getType());

        // taxonomic coverages tests
        assertNotNull(dataset.getTaxonomicCoverages());
        assertEquals(2, dataset.getTaxonomicCoverages().size());
        assertEquals("This is a general taxon coverage with only the scientific name", dataset.getTaxonomicCoverages().get(0).getDescription());
        assertEquals("Mammalia", dataset.getTaxonomicCoverages().get(0).getCoverages().get(0).getScientificName());
        assertEquals("Reptilia", dataset.getTaxonomicCoverages().get(0).getCoverages().get(1).getScientificName());
        assertEquals("Coleoptera", dataset.getTaxonomicCoverages().get(0).getCoverages().get(2).getScientificName());

        assertEquals("This is a second taxon coverage with all fields", dataset.getTaxonomicCoverages().get(1).getDescription());
        // 1st classification
        assertEquals("Class", dataset.getTaxonomicCoverages().get(1).getCoverages().get(0).getRank().getVerbatim());
        assertEquals(Rank.CLASS, dataset.getTaxonomicCoverages().get(1).getCoverages().get(0).getRank().getInterpreted());
        assertEquals("Aves", dataset.getTaxonomicCoverages().get(1).getCoverages().get(0).getScientificName());
        assertEquals("Birds", dataset.getTaxonomicCoverages().get(1).getCoverages().get(0).getCommonName());
        // 2nd classification
        assertEquals("Kingdom", dataset.getTaxonomicCoverages().get(1).getCoverages().get(1).getRank().getVerbatim());
        assertEquals(Rank.KINGDOM, dataset.getTaxonomicCoverages().get(1).getCoverages().get(1).getRank().getInterpreted());
        assertEquals("Plantae", dataset.getTaxonomicCoverages().get(1).getCoverages().get(1).getScientificName());
        assertEquals("Plants", dataset.getTaxonomicCoverages().get(1).getCoverages().get(1).getCommonName());
        // 3nd classification with uninterpretable Rank. Only the verbatim value is preserved
        assertEquals("kingggggggggggggdom", dataset.getTaxonomicCoverages().get(1).getCoverages().get(2).getRank().getVerbatim());
        assertNull(dataset.getTaxonomicCoverages().get(1).getCoverages().get(2).getRank().getInterpreted());
        assertEquals("Animalia", dataset.getTaxonomicCoverages().get(1).getCoverages().get(2).getScientificName());
        assertEquals("Animals", dataset.getTaxonomicCoverages().get(1).getCoverages().get(2).getCommonName());

        // sampling methods
        assertNotNull(dataset.getSamplingDescription());
        assertEquals(3, dataset.getSamplingDescription().getMethodSteps().size());
        assertEquals("Took picture, identified", dataset.getSamplingDescription().getMethodSteps().get(0));
        assertEquals("Themometer based test", dataset.getSamplingDescription().getMethodSteps().get(1));
        assertEquals("Visual based test\nand one more time", dataset.getSamplingDescription().getMethodSteps().get(2));
        assertEquals("Daily Obersevation of Pigeons Eating Habits", dataset.getSamplingDescription().getStudyExtent());
        assertEquals("44KHz is what a CD has... I was more like one a day if I felt like it", dataset.getSamplingDescription().getSampling());
        assertEquals("None", dataset.getSamplingDescription().getQualityControl());

        // Project (not included is attribute citableClassificationSystem)
        assertNotNull(dataset.getProject());
        assertEquals("Documenting Some Asian Birds and Insects", dataset.getProject().getTitle());
        assertNotNull(dataset.getProject().getContacts());
        assertEquals("Remsen", dataset.getProject().getContacts().get(0).getLastName());
        assertEquals(ContactType.PUBLISHER, dataset.getProject().getContacts().get(0).getType());
        assertEquals("My Deep Pockets", dataset.getProject().getFunding());
        assertEquals("Turkish Mountains", dataset.getProject().getStudyAreaDescription());
        assertEquals("This was done in Avian Migration patterns", dataset.getProject().getDesignDescription());

        assertEquals(Language.ENGLISH, dataset.getLanguage());

        // cal.clear();
        // // 2002-10-23T18:13:51
        // SimpleTimeZone tz = new SimpleTimeZone(1000 * 60 * 60, "berlin");
        // cal.setTimeZone(tz);
        // cal.set(2002, Calendar.OCTOBER, 23, 18, 13, 51);
        // cal.set(Calendar.MILLISECOND, 235);
        // assertEquals(cal.getTime(), dataset.getDateStamp());

        // Single Citation
        assertEquals("doi:tims-ident.2135.ex43.33.d", dataset.getCitation().getIdentifier());
        assertEquals("Tims assembled checklist", dataset.getCitation().getText());

        // Bibliographic citations
        assertNotNull(dataset.getBibliographicCitations());
        assertEquals(3, dataset.getBibliographicCitations().size());
        assertNotNull(dataset.getBibliographicCitations().get(0));
        assertEquals("title 1", dataset.getBibliographicCitations().get(0).getText());
        assertEquals("title 2", dataset.getBibliographicCitations().get(1).getText());
        assertEquals("title 3", dataset.getBibliographicCitations().get(2).getText());
        assertEquals("doi:tims-ident.2136.ex43.33.d", dataset.getBibliographicCitations().get(0).getIdentifier());
        assertEquals("doi:tims-ident.2137.ex43.33.d", dataset.getBibliographicCitations().get(1).getIdentifier());
        assertEquals("doi:tims-ident.2138.ex43.33.d", dataset.getBibliographicCitations().get(2).getIdentifier());

        // assertEquals("dataset", dataset.getHierarchyLevel());

        // Data description (Physical data aka external links)
        assertNotNull(dataset.getDataDescriptions());
        assertEquals(2, dataset.getDataDescriptions().size());
        assertEquals("INV-GCEM-0305a1_1_1.shp", dataset.getDataDescriptions().get(0).getName());
        assertEquals("ASCII", dataset.getDataDescriptions().get(0).getCharset());
        assertEquals("shapefile", dataset.getDataDescriptions().get(0).getFormat());
        assertEquals("2.0", dataset.getDataDescriptions().get(0).getFormatVersion());
        assertEquals(buildURI(
                "http://metacat.lternet.edu/knb/dataAccessServlet?docid=knb-lter-gce.109.10&urlTail=accession=INV-GCEM-0305a1&filename=INV-GCEM-0305a1_1_1.TXT"),
            dataset.getDataDescriptions().get(0).getUrl());
        assertEquals("INV-GCEM-0305a1_1_2.shp", dataset.getDataDescriptions().get(1).getName());
        assertEquals("ASCII", dataset.getDataDescriptions().get(1).getCharset());
        assertEquals("shapefile", dataset.getDataDescriptions().get(1).getFormat());
        assertEquals("2.0", dataset.getDataDescriptions().get(1).getFormatVersion());
        assertEquals(buildURI(
                "http://metacat.lternet.edu/knb/dataAccessServlet?docid=knb-lter-gce.109.10&urlTail=accession=INV-GCEM-0305a1&filename=INV-GCEM-0305a1_1_2.TXT"),
            dataset.getDataDescriptions().get(1).getUrl());

        assertEquals("Provide data to the whole world.", dataset.getPurpose());
        assertEquals("Where can the additional information possibly come from?!", dataset.getAdditionalInfo());

        // intellectual rights tests
        assertNotNull(dataset.getRights());
        assertTrue(dataset.getRights().startsWith("Owner grants"));
        assertTrue(dataset.getRights().endsWith("Site)."));

        // Collection
        Collection collection = dataset.getCollections().get(0);
        assertEquals(PreservationMethodType.ALCOHOL, collection.getSpecimenPreservationMethod());
        assertEquals("urn:lsid:tim.org:12:1", collection.getParentIdentifier());
        assertEquals("urn:lsid:tim.org:12:2", collection.getIdentifier());
        assertEquals("Mammals", collection.getName());
        // JGTI curatorial unit tests inside Collection
        assertNotNull(dataset.getCollections().get(0).getCuratorialUnits());
        assertEquals(5, dataset.getCollections().get(0).getCuratorialUnits().get(0).getCount());
        assertEquals(1, dataset.getCollections().get(0).getCuratorialUnits().get(0).getDeviation());
        assertEquals("SPECIMENS", dataset.getCollections().get(0).getCuratorialUnits().get(0).getTypeVerbatim());
        assertEquals("Drawers", dataset.getCollections().get(0).getCuratorialUnits().get(1).getTypeVerbatim());
        assertEquals(7, dataset.getCollections().get(0).getCuratorialUnits().get(1).getLower());
        assertEquals(2, dataset.getCollections().get(0).getCuratorialUnits().get(1).getUpper());
    }

    @Test
  public void testEmlParsingBreaking() throws IOException {
    // throws a ConversionException/Throwable that is caught - but build still returns the dataset populated partially
    Dataset dataset = DatasetParser.parse(MetadataType.EML, FileUtils.classpathStream("eml/sample-breaking.xml"));
    assertEquals("Estimates of walleye abundance for Oneida\n" + "      Lake, NY (1957-2008)", dataset.getTitle());
  }

  @Test
  public void testEmlParsingBreakingOnURLConversion() throws IOException {
    // Gracefully handles ConversionException/Throwable during conversion of URLs, and fully populates the dataset
    Dataset dataset = DatasetParser.parse(MetadataType.EML, FileUtils.classpathStream("eml/sample-breaking2.xml"));
    assertEquals("WII Herbarium Dataset", dataset.getTitle());
    assertEquals(buildURI("http://www.wii.gov.in"), dataset.getHomepage());
  }

  private URI buildURI(String uri) {
    try {
      return new URL(uri).toURI();
    } catch (URISyntaxException e) {
    } catch (MalformedURLException e) {
    }
    return null;
  }
}
