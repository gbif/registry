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
package org.gbif.registry.ws.it.collections.service.merge;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.service.collections.merge.CollectionMergeService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the {@link CollectionMergeService}. */
public class CollectionMergeServiceIT extends BaseMergeServiceIT<Collection> {

  private final CollectionMergeService collectionMergeService;
  private final CollectionService collectionService;
  private final InstitutionService institutionService;

  @Autowired
  public CollectionMergeServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      CollectionMergeService collectionMergeService,
      CollectionService collectionService,
      InstitutionService institutionService) {
    super(
        simplePrincipalProvider,
        collectionMergeService,
        collectionService,
        collectionService,
        collectionService,
        collectionService,
        collectionService,
        collectionService);
    this.collectionMergeService = collectionMergeService;
    this.collectionService = collectionService;
    this.institutionService = institutionService;
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
        () -> collectionMergeService.merge(toReplace.getKey(), replacement.getKey()));
  }

  @Override
  Collection createEntityToReplace() {
    Collection toReplace = new Collection();
    toReplace.setCode("c1");
    toReplace.setName("n1");
    toReplace.setDescription("description");
    toReplace.getEmail().add("a@a.com");
    toReplace.getEmail().add("b@a.com");
    toReplace.setGeographicCoverage("replaced geo");
    return toReplace;
  }

  @Override
  Collection createReplacement() {
    Collection replacement = new Collection();
    replacement.setCode("c2");
    replacement.setName("n2");
    replacement.getEmail().add("a@a.com");
    replacement.setGeographicCoverage("geo");
    return replacement;
  }

  @Override
  void extraAsserts(Collection replaced, Collection replacement, Collection replacementUpdated) {
    assertEquals(replacement.getKey(), replaced.getReplacedBy());
    assertEquals(2, replacementUpdated.getEmail().size());
    assertEquals(replacement.getGeographicCoverage(), replacementUpdated.getGeographicCoverage());
    assertEquals(replaced.getDescription(), replacementUpdated.getDescription());
  }
}
