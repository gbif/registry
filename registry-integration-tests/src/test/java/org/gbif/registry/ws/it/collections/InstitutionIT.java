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

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Institution;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.resources.collections.InstitutionResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the {@link InstitutionResource}. */
public class InstitutionIT extends ExtendedCollectionEntityTest<Institution> {

  private static final String NAME = "name";
  private static final String DESCRIPTION = "dummy description";
  private static final URI HOMEPAGE = URI.create("http://dummy");
  private static final String CODE_UPDATED = "code2";
  private static final String NAME_UPDATED = "name2";
  private static final String DESCRIPTION_UPDATED = "dummy description updated";
  private static final String ADDITIONAL_NAME = "additional name";

  // query params
  private static final String CODE_PARAM = "code";
  private static final String NAME_PARAM = "name";
  private static final String ALT_CODE_PARAM = "alternativeCode";

  @Autowired
  public InstitutionIT(
      MockMvc mockMvc,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      IdentityService identityService) {
    super(mockMvc, principalProvider, esServer, identityService, Institution.class);
  }

  @Test
  public void listTest() throws Exception {
    Institution institution1 = newEntity();
    institution1.setCode("c1");
    institution1.setName("n1");
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    institution1.setAddress(address);
    institution1.setAlternativeCodes(Collections.singletonMap("alt", "test"));
    UUID key1 = createEntityCall(institution1);

    Institution institution2 = newEntity();
    institution2.setCode("c2");
    institution2.setName("n2");
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    institution2.setAddress(address2);
    UUID key2 = createEntityCall(institution2);

    // query param
    assertEquals(2, listEntitiesCall(DEFAULT_QUERY_PARAMS.get()).getResults().size());
    assertEquals(2, listEntitiesCall(Q_SEARCH_PARAMS.apply("dummy")).getResults().size());

    // empty queries are ignored and return all elements
    assertEquals(2, listEntitiesCall(Q_SEARCH_PARAMS.apply("")).getResults().size());

    List<Institution> institutions = listEntitiesCall(Q_SEARCH_PARAMS.apply("city")).getResults();
    assertEquals(1, institutions.size());
    assertEquals(key1, institutions.get(0).getKey());

    institutions = listEntitiesCall(Q_SEARCH_PARAMS.apply("city2")).getResults();
    assertEquals(1, institutions.size());
    assertEquals(key2, institutions.get(0).getKey());

    assertEquals(2, listEntitiesCall(Q_SEARCH_PARAMS.apply("c")).getResults().size());
    assertEquals(2, listEntitiesCall(Q_SEARCH_PARAMS.apply("dum add")).getResults().size());
    assertEquals(0, listEntitiesCall(Q_SEARCH_PARAMS.apply("<")).getResults().size());
    assertEquals(0, listEntitiesCall(Q_SEARCH_PARAMS.apply("\"<\"")).getResults().size());
    assertEquals(2, listEntitiesCall(Q_SEARCH_PARAMS.apply(" ")).getResults().size());

    // code and name params
    Map<String, List<String>> params = DEFAULT_QUERY_PARAMS.get();
    params.put(CODE_PARAM, Collections.singletonList("c1"));
    assertEquals(1, listEntitiesCall(params).getResults().size());

    params = DEFAULT_QUERY_PARAMS.get();
    params.put(NAME_PARAM, Collections.singletonList("n2"));
    assertEquals(1, listEntitiesCall(params).getResults().size());

    params = DEFAULT_QUERY_PARAMS.get();
    params.put(CODE_PARAM, Collections.singletonList("c1"));
    params.put(NAME_PARAM, Collections.singletonList("n1"));
    assertEquals(1, listEntitiesCall(params).getResults().size());

    params.put(CODE_PARAM, Collections.singletonList("c2"));
    assertEquals(0, listEntitiesCall(params).getResults().size());

    // alternative code
    params = DEFAULT_QUERY_PARAMS.get();
    params.put(ALT_CODE_PARAM, Collections.singletonList("alt"));
    assertEquals(1, listEntitiesCall(params).getResults().size());

    params.put(ALT_CODE_PARAM, Collections.singletonList("foo"));
    assertEquals(0, listEntitiesCall(params).getResults().size());

    // update address
    institution2 = getEntityCall(key2);
    institution2.getAddress().setCity("city3");
    updateEntityCall(institution2);
    assertEquals(1, listEntitiesCall(Q_SEARCH_PARAMS.apply("city3")).getResults().size());

    deleteEntityCall(key2);
    assertEquals(0, listEntitiesCall(Q_SEARCH_PARAMS.apply("city3")).getResults().size());
  }

  @Test
  public void testSuggest() throws Exception {
    Institution institution1 = newEntity();
    institution1.setCode("II");
    institution1.setName("Institution name");
    UUID key1 = createEntityCall(institution1);

    Institution institution2 = newEntity();
    institution2.setCode("II2");
    institution2.setName("Institution name2");
    UUID key2 = createEntityCall(institution2);

    assertEquals(2, suggestCall("institution").size());
    assertEquals(2, suggestCall("II").size());
    assertEquals(1, suggestCall("II2").size());
    assertEquals(1, suggestCall("name2").size());
  }

  @Override
  protected Institution newEntity() {
    Institution institution = new Institution();
    institution.setCode(UUID.randomUUID().toString());
    institution.setName(NAME);
    institution.setDescription(DESCRIPTION);
    institution.setHomepage(HOMEPAGE);
    institution.setAdditionalNames(Collections.emptyList());
    return institution;
  }

  @Override
  protected void assertNewEntity(Institution institution) {
    assertEquals(NAME, institution.getName());
    assertEquals(DESCRIPTION, institution.getDescription());
    assertEquals(HOMEPAGE, institution.getHomepage());
    assertTrue(institution.getAdditionalNames().isEmpty());
  }

  @Override
  protected Institution updateEntity(Institution institution) {
    institution.setCode(CODE_UPDATED);
    institution.setName(NAME_UPDATED);
    institution.setDescription(DESCRIPTION_UPDATED);
    institution.setAdditionalNames(Arrays.asList(ADDITIONAL_NAME));
    return institution;
  }

  @Override
  protected void assertUpdatedEntity(Institution institution) {
    assertEquals(CODE_UPDATED, institution.getCode());
    assertEquals(NAME_UPDATED, institution.getName());
    assertEquals(DESCRIPTION_UPDATED, institution.getDescription());
    assertEquals(1, institution.getAdditionalNames().size());
    assertNotEquals(institution.getCreated(), institution.getModified());
  }

  @Override
  protected Institution newInvalidEntity() {
    return new Institution();
  }

  @Override
  protected String getBasePath() {
    return "/grscicoll/institution/";
  }
}
