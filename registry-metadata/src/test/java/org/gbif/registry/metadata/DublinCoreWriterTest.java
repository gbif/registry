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

import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.model.registry.eml.geospatial.BoundingBox;
import org.gbif.api.model.registry.eml.geospatial.GeospatialCoverage;
import org.gbif.api.model.registry.eml.temporal.DateRange;
import org.gbif.api.model.registry.eml.temporal.TemporalCoverage;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.Language;

import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for DublinCoreWriter.
 */
public class DublinCoreWriterTest {

  // Take it from the test/resources folder since the live one was down for days in April 2016
  // http://www.openarchives.org/OAI/2.0/oai_dc.xsd
  // Within the schema, a schemaLocation is changed from
  // http://dublincore.org/schemas/xmls/simpledc20021212.xsd,
  // which redirects to HTTPS.  The validator doesn't follow the redirect.
  private static final String OAI_2_0_DC_SCHEMA = "xsd/oai_dc.xsd";

  @Test
  public void testWrite() throws Exception {
    Dataset d = new Dataset();
    d.setKey(UUID.fromString("bdd601cc-00a7-431c-9724-d5b03170fcb2"));
    // d.setDoi(new DOI("10.1234/5679"));
    d.setTitle("This is a keyboard dataset");
    d.setDataLanguage(Language.FRENCH);

    Calendar calendar = Calendar.getInstance();
    calendar.set(2015, 8, 14);
    d.setPubDate(calendar.getTime());
    d.setDescription("Description d'un dataset sur les claviers qwerty");

    d.setHomepage(URI.create("http://www.gbif.org"));
    d.setPurpose("Le but de ce dataset est de ...");
    List<KeywordCollection> listKeyWordColl = new ArrayList<>();
    KeywordCollection keyword = new KeywordCollection();
    keyword.addKeyword("keyboard");
    keyword.addKeyword("qwerty");
    listKeyWordColl.add(keyword);
    d.setKeywordCollections(listKeyWordColl);

    // try additional properties
    Map<String, Object> additionalProperties = new HashMap<>();
    additionalProperties.put(DublinCoreWriter.ADDITIONAL_PROPERTY_OCC_COUNT, 3L);
    additionalProperties.put(
        DublinCoreWriter.ADDITIONAL_PROPERTY_DC_FORMAT, "application/dwca+zip");

    List<Citation> citationList = new ArrayList<>();
    Citation citation = new Citation();
    citation.setText("Qwerty U (2015). The World Register of Keyboards. 2015-08-09");
    citationList.add(citation);
    d.setBibliographicCitations(citationList);

    Contact originatorContact = new Contact();
    originatorContact.setFirstName("Carey");
    originatorContact.setLastName("Price");
    originatorContact.setType(ContactType.ORIGINATOR);
    originatorContact.setPrimary(true);

    // add the same name twice sith a different ContactType to make sure it will appear only once at
    // the end
    Contact metadataAuthorContact = new Contact();
    metadataAuthorContact.setFirstName("Carey");
    metadataAuthorContact.setLastName("Price");
    metadataAuthorContact.setType(ContactType.METADATA_AUTHOR);
    metadataAuthorContact.setPrimary(true);

    // ADMINISTRATIVE_POINT_OF_CONTACT should be displayed first in the generated DublinCore
    // document
    Contact administrativeContact = new Contact();
    administrativeContact.setFirstName("Patrick");
    administrativeContact.setLastName("Roy");
    administrativeContact.setType(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT);

    d.setContacts(
        Arrays.asList(originatorContact, metadataAuthorContact, administrativeContact));

    d.setGeographicCoverageDescription("Description de la portée");
    Date endDate = calendar.getTime();
    // subtract a day
    calendar.roll(Calendar.DAY_OF_MONTH, false);
    Date startDate = calendar.getTime();
    TemporalCoverage tp = new DateRange(startDate, endDate);
    d.setTemporalCoverages(Collections.singletonList(tp));

    d.setGeographicCoverageDescription("Plusieurs pays");
    GeospatialCoverage geospatialCoverage = new GeospatialCoverage();
    geospatialCoverage.setBoundingBox(new BoundingBox(-10, 10, 20, -20));
    geospatialCoverage.setDescription("Defined by bounding box");
    d.setGeographicCoverages(Collections.singletonList(geospatialCoverage));

    Organization organization = new Organization();
    organization.setTitle("Qwerty U");

    StringWriter writer = new StringWriter();
    DublinCoreWriter.newInstance().writeTo(organization, d, additionalProperties, writer);

    // Load test file
    String expectedContent =
        FileUtils.readFileToString(
            org.gbif.utils.file.FileUtils.getClasspathFile("dc/qwerty_dc.xml"),
            StandardCharsets.UTF_8);

    // compare without the whitespace characters
    String expectedFileContent = org.gbif.utils.text.StringUtils.deleteWhitespace(expectedContent);
    String actualFileContent = org.gbif.utils.text.StringUtils.deleteWhitespace(writer.toString());

    assertEquals(expectedFileContent, actualFileContent);

    // ensure we have a valid XML file according to the schema
    XMLValidator.assertXMLAgainstXSD(
        writer.toString(), org.gbif.utils.file.FileUtils.classpath2Filepath(OAI_2_0_DC_SCHEMA));
  }
}
