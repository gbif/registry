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
package org.gbif.registry.ws.it.collections.resource;

import org.gbif.api.model.collections.search.CollectionsSearchResponse;
import org.gbif.registry.search.dataset.service.collections.CollectionsSearchService;
import org.gbif.registry.ws.client.collections.CollectionsSearchClient;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class CollectionsSearchResourceTest extends BaseResourceIT {

  @MockBean private CollectionsSearchService collectionsSearchService;

  private final CollectionsSearchClient collectionsSearchClient;

  @Autowired
  public CollectionsSearchResourceTest(
      SimplePrincipalProvider simplePrincipalProvider,
      RequestTestFixture requestTestFixture,
      @LocalServerPort int localServerPort) {
    super(simplePrincipalProvider, requestTestFixture);
    this.collectionsSearchClient =
        prepareClient(
            TestConstants.TEST_GRSCICOLL_ADMIN, localServerPort, CollectionsSearchClient.class);
  }

  @Test
  public void searchTest() {
    String q = "foo";
    boolean highlight = true;
    int limit = 10;

    CollectionsSearchResponse response = new CollectionsSearchResponse();
    response.setCode("c1");
    response.setInstitutionKey(UUID.randomUUID());
    CollectionsSearchResponse.Match match = new CollectionsSearchResponse.Match();
    match.setField("field1");
    match.setSnippet("snippet");
    response.setMatches(Collections.singleton(match));

    when(collectionsSearchService.search(q, highlight, limit))
        .thenReturn(Collections.singletonList(response));

    List<CollectionsSearchResponse> responseReturned =
        collectionsSearchClient.searchCollections(q, highlight, limit);
    assertEquals(1, responseReturned.size());
    assertEquals(response, responseReturned.get(0));
  }
}
