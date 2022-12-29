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
package org.gbif.registry.ws.it.collections.resource;

import org.gbif.api.model.collections.lookup.AlternativeMatches;
import org.gbif.api.model.collections.lookup.CollectionMatched;
import org.gbif.api.model.collections.lookup.InstitutionMatched;
import org.gbif.api.model.collections.lookup.LookupParams;
import org.gbif.api.model.collections.lookup.LookupResult;
import org.gbif.api.model.collections.lookup.Match;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.service.collections.lookup.LookupService;
import org.gbif.registry.ws.client.collections.LookupClient;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.net.URI;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.CONCURRENT)
public class LookupResourceTest extends BaseResourceIT {

  @MockBean private LookupService lookupService;

  private final LookupClient lookupClient;

  @Autowired
  public LookupResourceTest(
      SimplePrincipalProvider simplePrincipalProvider,
      RequestTestFixture requestTestFixture,
      @LocalServerPort int localServerPort) {
    super(simplePrincipalProvider, requestTestFixture);
    this.lookupClient =
        prepareClient(TestConstants.TEST_GRSCICOLL_ADMIN, localServerPort, LookupClient.class);
  }

  @Test
  public void lookupTest() {
    UUID datasetKey = UUID.randomUUID();
    String institutionCode = "i1";
    String institutionId = "iid1";
    String ownerInstitutionCode = "owner";
    String collectionCode = "c1";
    String collectionId = "cid1";
    Country country = Country.DENMARK;
    boolean verbose = true;

    LookupParams params = new LookupParams();
    params.setDatasetKey(datasetKey);
    params.setInstitutionCode(institutionCode);
    params.setInstitutionId(institutionId);
    params.setOwnerInstitutionCode(ownerInstitutionCode);
    params.setCollectionCode(collectionCode);
    params.setCollectionId(collectionId);
    params.setCountry(country);
    params.setVerbose(verbose);

    LookupResult result = new LookupResult();
    InstitutionMatched institutionMatched = new InstitutionMatched();
    institutionMatched.setCode("c1");
    institutionMatched.setName("n1");
    institutionMatched.setActive(true);
    institutionMatched.setSelfLink(URI.create("http://test.com"));
    result.setInstitutionMatch(Match.exact(institutionMatched, Match.Reason.CODE_MATCH));

    CollectionMatched collectionMatched = new CollectionMatched();
    collectionMatched.setCode("c2");
    collectionMatched.setName("n2");
    collectionMatched.setActive(true);
    collectionMatched.setSelfLink(URI.create("http://test2.com"));
    result.setCollectionMatch(Match.exact(collectionMatched, Match.Reason.CODE_MATCH));

    AlternativeMatches alternativeMatches = new AlternativeMatches();
    alternativeMatches.setInstitutionMatches(
        Collections.singletonList(result.getInstitutionMatch()));
    alternativeMatches.setCollectionMatches(Collections.singletonList(result.getCollectionMatch()));
    result.setAlternativeMatches(alternativeMatches);

    when(lookupService.lookup(params)).thenReturn(result);

    LookupResult resultReturned =
        lookupClient.lookup(
            datasetKey,
            institutionCode,
            institutionId,
            ownerInstitutionCode,
            collectionCode,
            collectionId,
            country,
            verbose);
    assertEquals(result.getInstitutionMatch(), resultReturned.getInstitutionMatch());
    assertEquals(result.getCollectionMatch(), resultReturned.getCollectionMatch());
    assertEquals(
        result.getAlternativeMatches().getInstitutionMatches(),
        resultReturned.getAlternativeMatches().getInstitutionMatches());
    assertEquals(
        result.getAlternativeMatches().getCollectionMatches(),
        resultReturned.getAlternativeMatches().getCollectionMatches());
  }
}
