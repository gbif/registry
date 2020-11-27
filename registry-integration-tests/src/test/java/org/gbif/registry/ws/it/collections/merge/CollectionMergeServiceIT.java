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
package org.gbif.registry.ws.it.collections.merge;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.service.collections.merge.CollectionMergeService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the {@link CollectionMergeService}.
 */
public class CollectionMergeServiceIT extends BaseMergeServiceIT<Collection> {

  private final CollectionMergeService collectionMergeService;
  private final CollectionService collectionService;
  private final InstitutionService institutionService;
  private final PersonService personService;

  @Autowired
  public CollectionMergeServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer,
      CollectionMergeService collectionMergeService,
      CollectionService collectionService,
      InstitutionService institutionService,
      PersonService personService) {
    super(
        simplePrincipalProvider,
        esServer,
        collectionMergeService,
        collectionService,
        collectionService,
        collectionService,
        collectionService,
        collectionService,
        personService);
    this.collectionMergeService = collectionMergeService;
    this.collectionService = collectionService;
    this.institutionService = institutionService;
    this.personService = personService;
  }

  @Test
  public void primaryCollectionInPersonsTests() {
    Collection toReplace = createEntityToReplace();
    collectionService.create(toReplace);

    // contact that has the replaced collection as primary collection
    Person p3 = new Person();
    p3.setFirstName("p3");
    p3.setPrimaryCollectionKey(toReplace.getKey());
    personService.create(p3);

    Collection replacement = createReplacement();
    collectionService.create(replacement);

    collectionMergeService.merge(toReplace.getKey(), replacement.getKey(), "test");

    Person p3Updated = personService.get(p3.getKey());
    assertEquals(replacement.getKey(), p3Updated.getPrimaryCollectionKey());
  }

  @Test
  public void extraPreconditionsTest() {
    Institution i1 = new Institution();
    i1.setCode("i1");
    i1.setName("i1");
    institutionService.create(i1);

    Institution i2 = new Institution();
    i2.setCode("i2");
    i2.setName("i2");
    institutionService.create(i2);

    Collection toReplace = createEntityToReplace();
    toReplace.setInstitutionKey(i1.getKey());
    collectionService.create(toReplace);

    Collection replacement = createReplacement();
    replacement.setInstitutionKey(i2.getKey());
    collectionService.create(replacement);

    assertThrows(
        IllegalArgumentException.class,
        () -> collectionMergeService.merge(toReplace.getKey(), replacement.getKey(), "user"));
  }

  @Override
  Collection createEntityToReplace() {
    Collection toReplace = new Collection();
    toReplace.setCode("c1");
    toReplace.setName("n1");
    toReplace.setDescription("description");
    toReplace.getEmail().add("a@a.com");
    toReplace.getEmail().add("b@a.com");
    toReplace.setGeography("replaced geo");
    return toReplace;
  }

  @Override
  Collection createReplacement() {
    Collection replacement = new Collection();
    replacement.setCode("c2");
    replacement.setName("n2");
    replacement.getEmail().add("a@a.com");
    replacement.setGeography("geo");
    return replacement;
  }

  @Override
  void extraAsserts(Collection replaced, Collection replacement, Collection replacementUpdated) {
    assertEquals(replacement.getKey(), replaced.getReplacedBy());
    assertEquals(2, replacementUpdated.getEmail().size());
    assertEquals(replacement.getGeography(), replacementUpdated.getGeography());
    assertEquals(replaced.getDescription(), replacementUpdated.getDescription());
  }
}
