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

    Dataset dataset = new Dataset();
    dataset.setTitle("Dataset to be cited");
    dataset.setVersion("2.1");
    dataset.setDoi(new DOI(TEST_PREFIX+"/abcd"));
    dataset.setPubDate(new Date(LocalDate.of(2009,2,8).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()));

    dataset.setType(DatasetType.CHECKLIST);

    Contact c = new Contact();
    c.setLastName("Doe");
    c.setFirstName("John D.");
    c.setType(ContactType.ORIGINATOR);

    dataset.getContacts().add(c);

    assertEquals("Doe J D (2009) Dataset to be cited. Version 2.1. Cited Organization. " +
            "Checklist Dataset http://doi.org/10.5072/abcd accessed via GBIF.org on 2017-02-15.",
            CitationGenerator.generateCitation(dataset,org));

  }
}
