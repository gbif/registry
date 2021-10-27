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
package org.gbif.registry.doi.converter;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.model.registry.eml.geospatial.BoundingBox;
import org.gbif.api.model.registry.eml.geospatial.GeospatialCoverage;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DatasetTestDataProvider {

  static Dataset prepareFullDataset(DOI doi) {
    Dataset dataset = new Dataset();
    dataset.setKey(UUID.fromString("4b3c4936-24fc-4cbd-886d-3f874a44e31f"));
    dataset.setType(DatasetType.OCCURRENCE);
    dataset.setTitle("my title");
    dataset.setCreated(Date.from(LocalDateTime.of(2019, 10, 2, 0, 0).toInstant(ZoneOffset.UTC)));
    dataset.setModified(Date.from(LocalDateTime.of(2019, 10, 3, 0, 0).toInstant(ZoneOffset.UTC)));
    dataset.setCreatedBy("Markus GBIF User");
    dataset.setLanguage(Language.NORWEGIAN);
    dataset.setDataLanguage(Language.NORWEGIAN);
    dataset.setLicense(License.CC0_1_0);

    KeywordCollection kc1 = new KeywordCollection();
    Set<String> keywords1 = new HashSet<>();
    keywords1.add("Specimen");
    kc1.setThesaurus("GBIF Dataset Subtype Vocabulary");
    kc1.setKeywords(keywords1);
    dataset.getKeywordCollections().add(kc1);

    KeywordCollection kc2 = new KeywordCollection();
    Set<String> keywords2 = new HashSet<>();
    keywords2.add("Occurrence");
    keywords2.add("Human observation");
    kc2.setThesaurus("GBIF Dataset Subtype Vocabulary");
    kc2.setKeywords(keywords2);
    dataset.getKeywordCollections().add(kc2);

    Contact contact1 = new Contact();
    contact1.setFirstName("Markus");
    contact1.setType(ContactType.ORIGINATOR);
    contact1.setOrganization("GBIF");
    contact1.setUserId(Arrays.asList(null, "https://orcid.org/0000-0000-0000-0001"));
    dataset.getContacts().add(contact1);

    Contact contact11 = new Contact();
    contact11.setFirstName("John");
    contact11.setType(ContactType.ORIGINATOR);
    contact11.setOrganization("GBIF");
    contact11.setUserId(Collections.singletonList(null));
    dataset.getContacts().add(contact11);

    Contact contact2 = new Contact();
    contact2.setFirstName("Hubert");
    contact2.setLastName("Reeves");
    contact2.setType(ContactType.METADATA_AUTHOR);
    contact2.setOrganization("DataCite");
    contact2.setUserId(Collections.singletonList("http://orcid.org/0000-0000-0000-0002"));
    dataset.getContacts().add(contact2);

    Contact contact22 = new Contact();
    contact22.setFirstName("Joe");
    contact22.setLastName("Smith");
    contact22.setOrganization("DataCite");
    contact22.setType(ContactType.METADATA_AUTHOR);
    dataset.getContacts().add(contact22);

    // add an author with no name
    Contact contact3 = new Contact();
    contact3.setType(ContactType.ORIGINATOR);
    contact3.setOrganization("GBIF");
    dataset.getContacts().add(contact3);

    // add a contributor with no name
    Contact contact4 = new Contact();
    contact4.setType(ContactType.METADATA_AUTHOR);
    contact4.setOrganization("DataCite");
    dataset.getContacts().add(contact4);

    dataset.setDoi(doi);
    dataset.setDescription("some description");
    List<GeospatialCoverage> geos = new ArrayList<>();
    dataset.setGeographicCoverages(geos);
    GeospatialCoverage g1 = new GeospatialCoverage();
    geos.add(g1);
    g1.setDescription("geo description");
    g1.setBoundingBox(new BoundingBox(1, 2, 3, 4));
    GeospatialCoverage g2 = new GeospatialCoverage();
    geos.add(g2);
    g2.setDescription("geo description 2");
    g2.setBoundingBox(new BoundingBox(5, 6, 7, 8));

    return dataset;
  }

  static Dataset prepareSimpleDataset(DOI doi) {
    Dataset dataset = new Dataset();
    dataset.setKey(UUID.randomUUID());
    dataset.setType(DatasetType.METADATA);
    dataset.setTitle("My Metadata");
    dataset.setCreated(new Date());
    dataset.setModified(new Date());
    dataset.setCreatedBy("Jim");
    dataset.setLanguage(Language.ENGLISH);
    dataset.setDataLanguage(Language.ENGLISH);
    dataset.setRights("Copyright ©");
    dataset.setDoi(doi);

    return dataset;
  }
}
