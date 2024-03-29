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
package org.gbif.registry.ws.it.collections.service;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.search.CollectionsSearchResponse;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.search.dataset.service.collections.CollectionsSearchService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static org.gbif.registry.domain.collections.TypeParam.COLLECTION;
import static org.gbif.registry.domain.collections.TypeParam.INSTITUTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the {@link CollectionsSearchService} * */
public class CollectionsSearchIT extends BaseServiceIT {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  private final CollectionsSearchService searchService;
  private final InstitutionService institutionService;
  private final CollectionService collectionService;

  private final Institution i1 = new Institution();
  private final Institution i11 = new Institution();
  private final Institution i2 = new Institution();
  private final Collection c1 = new Collection();
  private final Collection c2 = new Collection();

  @Autowired
  public CollectionsSearchIT(
      SimplePrincipalProvider simplePrincipalProvider,
      CollectionsSearchService collectionsSearchService,
      InstitutionService institutionService,
      CollectionService collectionService) {
    super(simplePrincipalProvider);
    this.searchService = collectionsSearchService;
    this.institutionService = institutionService;
    this.collectionService = collectionService;
  }

  @BeforeEach
  public void loadData() {
    i1.setCode("I1");
    i1.setName("Institution 1");
    Address addressI1 = new Address();
    addressI1.setCountry(Country.AFGHANISTAN);
    addressI1.setAddress("foo street");
    i1.setAddress(addressI1);
    i1.setDisplayOnNHCPortal(false);
    institutionService.create(i1);

    i11.setCode("I11");
    i11.setName("I11");
    institutionService.create(i11);

    i2.setCode("I2");
    i2.setName("Institution 2");
    i2.setDescription("different than i1");
    i2.setAlternativeCodes(Collections.singletonList(new AlternativeCode("II2", "test")));
    i2.getIdentifiers().add(new Identifier(IdentifierType.LSID, "lsid-inst"));
    institutionService.create(i2);

    c1.setCode("C1");
    c1.setName("Collection 1");
    c1.setInstitutionKey(i1.getKey());
    Address addressC1 = new Address();
    addressC1.setCountry(Country.SPAIN);
    addressC1.setCity("Oviedo");
    addressC1.setProvince("Asturias");
    addressC1.setAddress("fake street");
    c1.setAddress(addressC1);
    c1.setDisplayOnNHCPortal(false);
    collectionService.create(c1);

    c2.setCode("C2");
    c2.setName("Collection 2");
    c2.setInstitutionKey(i2.getKey());
    c2.setAlternativeCodes(Collections.singletonList(new AlternativeCode("CC2", "test")));
    c2.getIdentifiers().add(new Identifier(IdentifierType.LSID, "lsid-coll"));
    collectionService.create(c2);
  }

  @Test
  public void searchByCodeTest() {
    List<CollectionsSearchResponse> responses =
        searchService.search("I1", true, null, null, null, 10);
    assertEquals(3, responses.size());
    assertEquals(i1.getKey(), responses.get(0).getKey());
    assertEquals(1, responses.get(0).getMatches().size());

    responses = searchService.search("i1", true, null, null, null, 10);
    assertEquals(3, responses.size());
    assertEquals(i11.getKey(), responses.get(0).getKey());
    assertEquals(2, responses.get(0).getMatches().size());
  }

  @Test
  public void searchByNameTest() {
    List<CollectionsSearchResponse> responses =
        searchService.search("Collection", true, null, null, null, 10);
    assertEquals(2, responses.size());

    responses = searchService.search("Collection 2", true, null, null, null, 10);
    assertEquals(2, responses.size());
    assertEquals(1, responses.get(0).getMatches().size());
    assertEquals(1, responses.get(1).getMatches().size());

    responses = searchService.search("Colllection 1", true, null, null, null, 10);
    assertEquals(2, responses.size());
    assertEquals(1, responses.get(0).getMatches().size());
    assertEquals(1, responses.get(1).getMatches().size());
  }

  @Test
  public void searchByAlternativeCodesTest() {
    List<CollectionsSearchResponse> responses =
        searchService.search("II2", true, null, null, null, 10);
    assertEquals(1, responses.size());
    assertEquals(i2.getKey(), responses.get(0).getKey());

    responses = searchService.search("test", true, null, null, null, 10);
    assertEquals(0, responses.size());
  }

  @Test
  public void searchByAddressFieldsTest() {
    List<CollectionsSearchResponse> responses =
        searchService.search("street", true, null, null, null, 10);
    assertEquals(2, responses.size());

    responses = searchService.search(Country.SPAIN.getIso2LetterCode(), true, null, null, null, 10);
    assertEquals(1, responses.size());
    assertEquals(c1.getKey(), responses.get(0).getKey());
    assertEquals(1, responses.get(0).getMatches().size());

    responses = searchService.search("oviedo", true, null, null, null, 10);
    assertEquals(1, responses.size());
    assertEquals(c1.getKey(), responses.get(0).getKey());
    assertEquals(1, responses.get(0).getMatches().size());

    responses = searchService.search("street asturias", true, null, null, null, 10);
    assertEquals(1, responses.size());
    assertEquals(c1.getKey(), responses.get(0).getKey());
    assertEquals(2, responses.get(0).getMatches().size());
  }

  @Test
  public void searchWithoutHighlightTest() {
    List<CollectionsSearchResponse> responses =
        searchService.search("Collection", false, null, null, null, 10);
    assertNull(responses.get(0).getMatches());
  }

  @Test
  public void noMatchesTest() {
    List<CollectionsSearchResponse> responses =
        searchService.search("nothing", false, null, null, null, 10);
    assertEquals(0, responses.size());

    responses = searchService.search("collection made up", false, null, null, null, 10);
    assertEquals(0, responses.size());

    responses = searchService.search("I1 made up", false, null, null, null, 10);
    assertEquals(0, responses.size());
  }

  @Test
  public void displayOnNHCPortalTest() {
    List<CollectionsSearchResponse> responses =
        searchService.search(null, false, null, true, null, 10);
    assertEquals(3, responses.size());

    responses = searchService.search(null, false, null, false, null, 10);
    assertEquals(2, responses.size());
  }

  @Test
  public void typeParamTest() {
    List<CollectionsSearchResponse> responses =
        searchService.search(null, false, INSTITUTION, null, null, 10);
    assertEquals(3, responses.size());
    assertTrue(responses.stream().allMatch(d -> d.getType().equals("institution")));

    responses = searchService.search(null, false, COLLECTION, null, null, 10);
    assertEquals(2, responses.size());
    assertTrue(responses.stream().allMatch(d -> d.getType().equals("collection")));

    responses = searchService.search(null, false, null, null, null, 10);
    assertEquals(5, responses.size());
  }

  @Test
  public void countryFilterTest() {
    List<CollectionsSearchResponse> responses =
        searchService.search(null, true, null, null, Country.SPAIN, 10);
    assertEquals(1, responses.size());
    assertEquals(c1.getKey(), responses.get(0).getKey());

    responses = searchService.search(null, true, null, null, Country.DENMARK, 10);
    assertEquals(0, responses.size());

    responses = searchService.search(null, true, INSTITUTION, null, Country.SPAIN, 10);
    assertEquals(0, responses.size());

    responses = searchService.search("I1", true, null, null, Country.SPAIN, 10);
    assertEquals(0, responses.size());

    responses = searchService.search("C1", true, null, null, Country.SPAIN, 10);
    assertEquals(1, responses.size());
    assertEquals(c1.getKey(), responses.get(0).getKey());
    assertEquals(1, responses.get(0).getMatches().size());

    responses = searchService.search("C1", true, null, null, Country.DENMARK, 10);
    assertEquals(0, responses.size());
  }
}
