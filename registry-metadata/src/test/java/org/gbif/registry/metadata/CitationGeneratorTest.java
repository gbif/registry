package org.gbif.registry.metadata;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.DatasetType;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.Test;

import static org.gbif.api.model.common.DOI.TEST_PREFIX;
import static org.gbif.registry.metadata.CitationGenerator.getAuthors;

import static junit.framework.TestCase.assertEquals;

/**
 * Unit tests related to {@link CitationGenerator}.
 */
public class CitationGeneratorTest {

  @Test
  public void testAuthorNames() {
    Contact c = new Contact();
    c.setLastName("Doe");
    c.setFirstName("John D.");
    assertEquals("Doe J D", CitationGenerator.getAuthorName(c));

    //test with missing first name
    c = new Contact();
    c.setLastName("Doe");
    c.setOrganization("Awesome Organization");
    assertEquals("Doe", CitationGenerator.getAuthorName(c));

    //test with missing parts
    c = new Contact();
    c.setFirstName("John");
    c.setOrganization("Awesome Organization");
    assertEquals("Awesome Organization", CitationGenerator.getAuthorName(c));
  }

  @Test
  public void testCompleteCitation() {

    Organization org = new Organization();
    org.setTitle("Cited Organization");

    Dataset dataset = getTestDatasetObject();

    Contact c = new Contact();
    c.setLastName("Doe");
    c.setFirstName("John D.");
    c.setType(ContactType.ORIGINATOR);
    dataset.getContacts().add(c);

    assertEquals("Doe J D (2009). Dataset to be cited. Version 2.1. Cited Organization. " +
            "Checklist Dataset http://doi.org/10.5072/abcd accessed via GBIF.org on " + LocalDate.now().toString() + ".",
            CitationGenerator.generateCitation(dataset,org));
  }

  @Test
  public void testCompleteCitationNoYear() {
    Organization org = new Organization();
    org.setTitle("Cited Organization");

    Dataset dataset = getTestDatasetObject();
    dataset.setPubDate(null);
    Contact c = new Contact();
    c.setLastName("Doe");
    c.setFirstName("John");
    c.setType(ContactType.ORIGINATOR);
    dataset.getContacts().add(c);

    assertEquals("Doe J. Dataset to be cited. Version 2.1. Cited Organization. " +
                    "Checklist Dataset http://doi.org/10.5072/abcd accessed via GBIF.org on " + LocalDate.now().toString() + ".",
            CitationGenerator.generateCitation(dataset,org));
  }

  @Test
  public void testCompleteCitationAuthorMultipleRoles() {
    Organization org = new Organization();
    org.setTitle("Cited Organization");

    Dataset dataset = getTestDatasetObject();

    Contact c = new Contact();
    c.setLastName("Doe");
    c.setFirstName("John D.");
    c.setType(ContactType.ORIGINATOR);

    Contact c1 = new Contact();
    c1.setLastName("Carey");
    c1.setFirstName("Jim");
    c1.setType(ContactType.PROGRAMMER);

    Contact c2 = new Contact();
    c2.setLastName("Doe");
    c2.setFirstName("John D.");
    c2.setType(ContactType.METADATA_AUTHOR);

    dataset.getContacts().add(c);
    dataset.getContacts().add(c2);

    assertEquals("Doe J D (2009). Dataset to be cited. Version 2.1. Cited Organization. " +
                    "Checklist Dataset http://doi.org/10.5072/abcd accessed via GBIF.org on " +
            LocalDate.now().toString() + ".",
            CitationGenerator.generateCitation(dataset,org));
  }

  @Test
  public void testAuthors(){
    Organization org = new Organization();
    org.setTitle("Cited Organization");

    Dataset dataset = getTestDatasetObject();

    Contact c = new Contact();
    c.setLastName("Doe");
    c.setFirstName("John D.");
    c.setType(ContactType.ORIGINATOR);
    dataset.getContacts().add(c);

    //author with incomplete name
    c = new Contact();
    c.setLastName("Doe");
    c.setOrganization("Awesome Organization");
    c.setType(ContactType.ORIGINATOR);
    dataset.getContacts().add(c);

    //not an author
    c = new Contact();
    c.setLastName("Last");
    c.setFirstName("Programmer");
    c.setType(ContactType.PROGRAMMER);
    dataset.getContacts().add(c);

    //we expect 2 authors
    assertEquals(2, getAuthors(dataset.getContacts()).size());

    //but, we can only generate the name for one of them
    assertEquals(1, CitationGenerator.generateAuthorsName(getAuthors(dataset.getContacts())).size());
  }

  private Dataset getTestDatasetObject() {
    Dataset dataset = new Dataset();
    dataset.setTitle("Dataset to be cited");
    dataset.setVersion("2.1");
    dataset.setDoi(new DOI(TEST_PREFIX+"/abcd"));
    dataset.setPubDate(new Date(LocalDate.of(2009,2,8).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()));

    dataset.setType(DatasetType.CHECKLIST);

    return dataset;
  }
}
