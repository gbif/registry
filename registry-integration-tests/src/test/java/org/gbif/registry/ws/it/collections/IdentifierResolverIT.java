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
package org.gbif.registry.ws.it.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.gbif.registry.ws.util.GrscicollUtils.GRSCICOLL_PATH;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests the {@link org.gbif.registry.ws.resources.collections.IdentifierResolverResource}. */
public class IdentifierResolverIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = TestCaseDatabaseInitializer.builder()
    .dataSource(database.getTestDatabase())
    .build();

  private static final String BASE_URL = "/" + GRSCICOLL_PATH + "/resolve";
  private static final String IDENTIFIER_PARAM = "identifier";
  private static final String TEST_USER = "test";

  private static final String IDENTIFIER1 = "http://grbio.org/cool/g9da-xpan";
  private static final String IDENTIFIER2 = "urn:lsid:biocol.org:col:35158";
  private static final String IDENTIFIER3 = "http://grscicoll.org/cool/kx98-stkb";
  private static final String IDENTIFIER4 = "http://usfsc.grscicoll.org/cool/i6ah-3d5y";

  private MockMvc mockMvc;
  private CollectionService collectionService;
  private InstitutionService institutionService;

  @Autowired
  public IdentifierResolverIT(
      MockMvc mockMvc,
      CollectionService collectionService,
      InstitutionService institutionService,
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer) {
    super(simplePrincipalProvider, esServer);
    this.mockMvc = mockMvc;
    this.collectionService = collectionService;
    this.institutionService = institutionService;
  }

  @Test
  public void findCollectionByIdentifierTest() throws Exception {
    // State
    Collection collection = new Collection();
    collection.setCode("c1");
    collection.setName("col1");
    collection.setCreatedBy(TEST_USER);
    collection.setModifiedBy(TEST_USER);
    UUID collectionKey = collectionService.create(collection);

    Collection deleted = new Collection();
    deleted.setCode("c2");
    deleted.setName("col2");
    deleted.setCreatedBy(TEST_USER);
    deleted.setModifiedBy(TEST_USER);
    UUID deletedKey = collectionService.create(deleted);
    collectionService.delete(deletedKey);

    // add identifier to collection
    Identifier id1 = new Identifier(IdentifierType.GRSCICOLL_URI, IDENTIFIER1);
    id1.setCreatedBy(TEST_USER);
    collectionService.addIdentifier(collectionKey, id1);

    Identifier idDeleted = new Identifier(IdentifierType.GRSCICOLL_URI, IDENTIFIER1);
    idDeleted.setCreatedBy(TEST_USER);
    collectionService.addIdentifier(deletedKey, idDeleted);

    // there could be duplicates since we don't check it
    Identifier id2 = new Identifier(IdentifierType.GRSCICOLL_URI, IDENTIFIER1);
    id2.setCreatedBy(TEST_USER);
    collectionService.addIdentifier(collectionKey, id2);

    // find collection by cool URI and env
    mockMvc
        .perform(get(BASE_URL).queryParam(IDENTIFIER_PARAM, IDENTIFIER1.replace("http://", "dev.")))
        .andExpect(status().isSeeOther())
        .andExpect(header().string("Location", endsWith("/collection/" + collectionKey)));

    // find collection by cool URI without env
    mockMvc
        .perform(get(BASE_URL).queryParam(IDENTIFIER_PARAM, IDENTIFIER1))
        .andExpect(status().isSeeOther())
        .andExpect(header().string("Location", endsWith("/collection/" + collectionKey)));
  }

  @Test
  public void findInstitutionByIdentifiersTest() throws Exception {
    // State
    Institution institution = new Institution();
    institution.setCode("i1");
    institution.setName("inst1");
    institution.setCreatedBy(TEST_USER);
    institution.setModifiedBy(TEST_USER);
    UUID institutionKey = institutionService.create(institution);

    Institution deleted = new Institution();
    deleted.setCode("i2");
    deleted.setName("inst2");
    deleted.setCreatedBy(TEST_USER);
    deleted.setModifiedBy(TEST_USER);
    UUID deletedKey = institutionService.create(deleted);
    institutionService.delete(deletedKey);

    // add identifiers to institution
    Identifier id3 = new Identifier(IdentifierType.LSID, IDENTIFIER2);
    id3.setCreatedBy(TEST_USER);
    institutionService.addIdentifier(institutionKey, id3);

    Identifier idDeleted = new Identifier(IdentifierType.LSID, IDENTIFIER2);
    idDeleted.setCreatedBy(TEST_USER);
    institutionService.addIdentifier(deletedKey, idDeleted);

    Identifier id4 = new Identifier(IdentifierType.GRSCICOLL_URI, IDENTIFIER3);
    id4.setCreatedBy(TEST_USER);
    institutionService.addIdentifier(institutionKey, id4);

    Identifier id5 = new Identifier(IdentifierType.GRSCICOLL_URI, IDENTIFIER4);
    id5.setCreatedBy(TEST_USER);
    institutionService.addIdentifier(institutionKey, id5);

    // find institution by LSID
    mockMvc
        .perform(get(BASE_URL).queryParam(IDENTIFIER_PARAM, IDENTIFIER2))
        .andExpect(status().isSeeOther())
        .andExpect(header().string("Location", endsWith("/institution/" + institutionKey)));

    // find institution by GrSciColl URI and env
    mockMvc
        .perform(get(BASE_URL).queryParam(IDENTIFIER_PARAM, IDENTIFIER3.replace("http://", "dev.")))
        .andExpect(status().isSeeOther())
        .andExpect(header().string("Location", endsWith("/institution/" + institutionKey)));

    // find institution by USFSC URI
    mockMvc
        .perform(get(BASE_URL).queryParam(IDENTIFIER_PARAM, IDENTIFIER4.replace("http://", "")))
        .andExpect(status().isSeeOther())
        .andExpect(header().string("Location", endsWith("/institution/" + institutionKey)));
  }

  @Test
  public void unknownIdentifier() throws Exception {
    mockMvc.perform(get(BASE_URL + "dev.grbio.org/cool/foo")).andExpect(status().isNotFound());
  }
}
