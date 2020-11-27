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
import org.gbif.registry.service.collections.merge.InstitutionMergeService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests the {@link InstitutionMergeService}. */
public class InstitutionMergeServiceIT extends BaseMergeServiceIT<Institution> {

  private final InstitutionMergeService institutionMergeService;
  private final InstitutionService institutionService;
  private final CollectionService collectionService;
  private final PersonService personService;

  @Autowired
  public InstitutionMergeServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer,
      InstitutionMergeService institutionMergeService,
      InstitutionService institutionService,
      CollectionService collectionService,
      PersonService personService) {
    super(
        simplePrincipalProvider,
        esServer,
        institutionMergeService,
        institutionService,
        institutionService,
        institutionService,
        institutionService,
        institutionService,
        personService);
    this.institutionMergeService = institutionMergeService;
    this.institutionService = institutionService;
    this.collectionService = collectionService;
    this.personService = personService;
  }

  @Test
  public void mergeWithCollectionsAndPrimaryInstitutionInContactsTest() {
    Institution toReplace = createEntityToReplace();
    institutionService.create(toReplace);

    // contact that has the replaced collection as primary collection
    Person p3 = new Person();
    p3.setFirstName("p3");
    p3.setPrimaryInstitutionKey(toReplace.getKey());
    personService.create(p3);

    // collections
    Collection c1 = new Collection();
    c1.setCode("c1");
    c1.setName("n1");
    c1.setInstitutionKey(toReplace.getKey());
    collectionService.create(c1);

    Institution replacement = createReplacement();
    institutionService.create(replacement);

    institutionMergeService.merge(toReplace.getKey(), replacement.getKey(), "test");

    Person p3Updated = personService.get(p3.getKey());
    assertEquals(replacement.getKey(), p3Updated.getPrimaryInstitutionKey());

    Collection c1Updated = collectionService.get(c1.getKey());
    assertEquals(replacement.getKey(), c1Updated.getInstitutionKey());
  }

  @Override
  Institution createEntityToReplace() {
    Institution toReplace = new Institution();
    toReplace.setCode("c1");
    toReplace.setName("n1");
    toReplace.setDescription("description");
    toReplace.getEmail().add("a@a.com");
    toReplace.getEmail().add("b@a.com");
    toReplace.setGeographicDescription("replaced geo");
    return toReplace;
  }

  @Override
  Institution createReplacement() {
    Institution replacement = new Institution();
    replacement.setCode("c2");
    replacement.setName("n2");
    replacement.getEmail().add("a@a.com");
    replacement.setGeographicDescription("geo");
    return replacement;
  }

  @Override
  void extraAsserts(Institution replaced, Institution replacement, Institution replacementUpdated) {
    assertEquals(replacementUpdated.getKey(), replaced.getReplacedBy());
    assertEquals(2, replacementUpdated.getEmail().size());
    assertEquals(
        replacement.getGeographicDescription(), replacementUpdated.getGeographicDescription());
    assertEquals(replaced.getDescription(), replacementUpdated.getDescription());
  }
}
