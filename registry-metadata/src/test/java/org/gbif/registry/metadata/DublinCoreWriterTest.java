package org.gbif.registry.metadata;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.Language;

import org.junit.Test;
import java.io.StringWriter;
import java.net.URI;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;


import static org.junit.Assert.assertEquals;

/**
 * Test class for DublinCoreWriter.
 *
 * @author cgendreau
 */
public class DublinCoreWriterTest {

  private static final String OAI_DC_SCHEMA = "http://www.openarchives.org/OAI/2.0/oai_dc.xsd";

  @Test
  public void testWrite() throws Exception {
    Dataset d = new Dataset();
    d.setKey(UUID.randomUUID());
    d.setDoi(new DOI("10.1234/5679"));
    d.setTitle("This is a keyboard dataset");
    d.setDataLanguage(Language.FRENCH);

    Calendar calendar = Calendar.getInstance();
    calendar.set(2015, 8, 14);
    d.setPubDate(calendar.getTime());
    d.setDescription("Description d'un dataset sur les claviers qwerty");

    d.setHomepage(URI.create("http:///www.gbif.org"));

    List<KeywordCollection> listKeyWordColl = Lists.newArrayList();
    KeywordCollection keyword = new KeywordCollection();
    keyword.addKeyword("keyboard");
    keyword.addKeyword("qwerty");
    listKeyWordColl.add(keyword);
    d.setKeywordCollections(listKeyWordColl);

    List<Citation> citationList = Lists.newArrayList();
    Citation citation = new Citation();
    citation.setText("Qwerty U (2015). The World Register of Keyboards. 2015-08-09");
    citationList.add(citation);
    d.setBibliographicCitations(citationList);

    Contact contact = new Contact();
    contact.setFirstName("Carey");
    contact.setLastName("Price");
    contact.setType(ContactType.ORIGINATOR);
    contact.setPrimary(true);
    d.setContacts(Lists.newArrayList(contact));

    Organization organization = new Organization();
    organization.setTitle("Qwerty U");

    StringWriter writer = new StringWriter();
    DublinCoreWriter.write(organization, d, writer);

    //Load test file
    String expectedContent = FileUtils.readFileToString(org.gbif.utils.file.FileUtils.getClasspathFile("dc/qwerty_dc.xml"));
    //compare without the 'newline' character(s)
    assertEquals(StringUtils.chomp(expectedContent), StringUtils.chomp(writer.toString()));

    // ensure we have a valid XML file according to the schema
    XMLValidator.assertXMLAgainstXSD(writer.toString(), OAI_DC_SCHEMA);

  }

}