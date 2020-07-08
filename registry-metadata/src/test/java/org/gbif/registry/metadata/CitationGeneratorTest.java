/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
import static org.junit.Assert.assertEquals;

/** Unit tests related to {@link CitationGenerator}. */
public class CitationGeneratorTest {

  @Test
  public void testAuthorNames() {
    Contact c = new Contact();
    c.setLastName("Doe");
    c.setFirstName("John D.");
    assertEquals("Doe J D", CitationGenerator.getAuthorName(c));

    // test with missing first name
    c = new Contact();
    c.setLastName("Doe");
    c.setOrganization("Awesome Organization");
    assertEquals("Doe", CitationGenerator.getAuthorName(c));

    // test with missing parts
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
    dataset.getContacts().add(createContact("John D.", "Doe", ContactType.ORIGINATOR));

    assertEquals(
        "Doe J D (2009). Dataset to be cited. Version 2.1. Cited Organization. "
            + "Sampling event dataset https://doi.org/10.21373/abcd accessed via GBIF.org on "
            + LocalDate.now(ZoneId.of("UTC")).toString()
            + ".",
        CitationGenerator.generateCitation(dataset, org));
  }

  @Test
  public void testCompleteCitationUserWithoutName() {
    Organization org = new Organization();
    org.setTitle("Cited Organization");

    Dataset dataset = getTestDatasetObject();
    dataset.getContacts().add(createContact("John D.", "Doe", ContactType.ORIGINATOR));
    dataset.getContacts().add(createContact("  ", "Smith", ContactType.ORIGINATOR));
    dataset.getContacts().add(createContact("John", null, ContactType.ORIGINATOR));
    dataset.getContacts().add(createContact(null, "Mendez", ContactType.ORIGINATOR));

    assertEquals(
        "Doe J D, Smith, Mendez (2009). Dataset to be cited. Version 2.1. Cited Organization. "
            + "Sampling event dataset https://doi.org/10.21373/abcd accessed via GBIF.org on "
            + LocalDate.now(ZoneId.of("UTC")).toString()
            + ".",
        CitationGenerator.generateCitation(dataset, org));
  }

  @Test
  public void testCompleteCitationNoAuthors() {
    Organization org = new Organization();
    org.setTitle("Cited Organization");

    Dataset dataset = getTestDatasetObject();
    dataset
        .getContacts()
        .add(
            createContact(
                null,
                null,
                "We are not using this field int the citation",
                ContactType.ORIGINATOR));

    assertEquals(
        "Cited Organization (2009). Dataset to be cited. Version 2.1. "
            + "Sampling event dataset https://doi.org/10.21373/abcd accessed via GBIF.org on "
            + LocalDate.now(ZoneId.of("UTC")).toString()
            + ".",
        CitationGenerator.generateCitation(dataset, org));
  }

  @Test
  public void testCompleteCitationNoYear() {
    Organization org = new Organization();
    org.setTitle("Cited Organization");

    Dataset dataset = getTestDatasetObject();
    dataset.setPubDate(null);
    dataset.getContacts().add(createContact("John", "Doe", ContactType.ORIGINATOR));

    assertEquals(
        "Doe J. Dataset to be cited. Version 2.1. Cited Organization. "
            + "Sampling event dataset https://doi.org/10.21373/abcd accessed via GBIF.org on "
            + LocalDate.now(ZoneId.of("UTC")).toString()
            + ".",
        CitationGenerator.generateCitation(dataset, org));
  }

  @Test
  public void testCompleteCitationAuthorMultipleRoles() {
    Organization org = new Organization();
    org.setTitle("Cited Organization");

    Dataset dataset = getTestDatasetObject();

    dataset.getContacts().add(createContact("John D.", "Doe", ContactType.ORIGINATOR));
    dataset.getContacts().add(createContact("Jim", "Carey", ContactType.PROGRAMMER));
    dataset.getContacts().add(createContact("John D.", "Doe", ContactType.METADATA_AUTHOR));

    assertEquals(
        "Doe J D (2009). Dataset to be cited. Version 2.1. Cited Organization. "
            + "Sampling event dataset https://doi.org/10.21373/abcd accessed via GBIF.org on "
            + LocalDate.now(ZoneId.of("UTC")).toString()
            + ".",
        CitationGenerator.generateCitation(dataset, org));
  }

  @Test
  public void testCompleteCitationNoOriginator() {
    Organization org = new Organization();
    org.setTitle("Cited Organization");
    Dataset dataset = getTestDatasetObject();
    dataset.getContacts().add(createContact("John D.", "Doe", ContactType.METADATA_AUTHOR));

    assertEquals(
        "Cited Organization (2009). Dataset to be cited. Version 2.1. "
            + "Sampling event dataset https://doi.org/10.21373/abcd accessed via GBIF.org on "
            + LocalDate.now(ZoneId.of("UTC")).toString()
            + ".",
        CitationGenerator.generateCitation(dataset, org));
  }

  @Test
  public void testCompleteCitationOriginatorNoName() {
    Organization org = new Organization();
    org.setTitle("Cited Organization");
    Dataset dataset = getTestDatasetObject();

    dataset.getContacts().add(createContact(null, null, "Test Org.", ContactType.ORIGINATOR));
    dataset.getContacts().add(createContact("John D.", "Doe", ContactType.METADATA_AUTHOR));

    assertEquals(
        "Cited Organization (2009). Dataset to be cited. Version 2.1. "
            + "Sampling event dataset https://doi.org/10.21373/abcd accessed via GBIF.org on "
            + LocalDate.now(ZoneId.of("UTC")).toString()
            + ".",
        CitationGenerator.generateCitation(dataset, org));
  }

  @Test
  public void testAuthors() {
    Organization org = new Organization();
    org.setTitle("Cited Organization");

    Dataset dataset = getTestDatasetObject();

    dataset.getContacts().add(createContact("John D.", "Doe", ContactType.ORIGINATOR));
    dataset
        .getContacts()
        .add(createContact("John D.", "Doe", "Awesome Organization", ContactType.ORIGINATOR));
    // author with incomplete name
    dataset.getContacts().add(createContact("Programmer", "Last", ContactType.PROGRAMMER));

    // we expect 1 author since the names (first and last) are mandatory
    assertEquals(1, getAuthors(dataset.getContacts()).size());

    // but, we can only generate the name for one of them
    assertEquals(
        1, CitationGenerator.generateAuthorsName(getAuthors(dataset.getContacts())).size());
  }

  private Dataset getTestDatasetObject() {
    Dataset dataset = new Dataset();
    dataset.setTitle("Dataset to be cited");
    dataset.setVersion("2.1");
    dataset.setDoi(new DOI(TEST_PREFIX + "/abcd"));
    dataset.setPubDate(
        new Date(
            LocalDate.of(2009, 2, 8).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()));

    dataset.setType(DatasetType.SAMPLING_EVENT);

    return dataset;
  }

  private Contact createContact(String firstName, String lastName, ContactType ct) {
    return createContact(firstName, lastName, null, ct);
  }

  private Contact createContact(
      String firstName, String lastName, String organization, ContactType ct) {
    Contact c = new Contact();
    c.setFirstName(firstName);
    c.setLastName(lastName);
    c.setOrganization(organization);
    c.setType(ct);
    return c;
  }
}
