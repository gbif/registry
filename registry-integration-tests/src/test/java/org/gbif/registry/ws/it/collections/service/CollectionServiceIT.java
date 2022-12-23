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

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.duplicates.Duplicate;
import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.persistence.mapper.collections.params.DuplicatesSearchParams;
import org.gbif.registry.service.collections.duplicates.CollectionDuplicatesService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the {@link CollectionService}. */
// @Execution(ExecutionMode.CONCURRENT)
public class CollectionServiceIT extends BaseCollectionEntityServiceIT<Collection> {

  private final CollectionService collectionService;
  private final CollectionDuplicatesService duplicatesService;
  private final InstitutionService institutionService;

  @Autowired
  public CollectionServiceIT(
      InstitutionService institutionService,
      CollectionService collectionService,
      DatasetService datasetService,
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService,
      SimplePrincipalProvider principalProvider,
      CollectionDuplicatesService duplicatesService) {
    super(
        collectionService,
        datasetService,
        nodeService,
        organizationService,
        installationService,
        principalProvider,
        duplicatesService,
        Collection.class);
    this.collectionService = collectionService;
    this.duplicatesService = duplicatesService;
    this.institutionService = institutionService;
  }

  @Test
  @Execution(ExecutionMode.CONCURRENT)
  public void createCollectionWithoutCodeTest() {
    Collection c = testData.newEntity();
    c.setCode(null);
    assertThrows(ValidationException.class, () -> collectionService.create(c));
  }

  @Test
  @Execution(ExecutionMode.CONCURRENT)
  public void updateCollectionWithoutCodeTest() {
    Collection c = testData.newEntity();
    UUID key = collectionService.create(c);

    Collection created = collectionService.get(key);
    created.setCode(null);
    assertThrows(IllegalArgumentException.class, () -> collectionService.update(created));
  }

  @Test
  @Execution(ExecutionMode.CONCURRENT)
  public void updateAndReplaceTest() {
    Collection c = testData.newEntity();
    UUID key = collectionService.create(c);

    Collection created = collectionService.get(key);
    created.setReplacedBy(UUID.randomUUID());
    assertThrows(IllegalArgumentException.class, () -> collectionService.update(created));
  }

  @Test
  @Execution(ExecutionMode.CONCURRENT)
  public void possibleDuplicatesTest() {
    testDuplicatesCommonCases();

    DuplicatesSearchParams params = new DuplicatesSearchParams();
    params.setSameInstitutionKey(true);
    params.setSameCode(true);

    DuplicatesResult result = duplicatesService.findPossibleDuplicates(params);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(2, result.getDuplicates().get(0).size());

    Set<UUID> keysFound =
        result.getDuplicates().get(0).stream()
            .map(Duplicate::getInstitutionKey)
            .collect(Collectors.toSet());
    params.setInInstitutions(new ArrayList<>(keysFound));
    result = duplicatesService.findPossibleDuplicates(params);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(2, result.getDuplicates().get(0).size());

    params.setInInstitutions(null);
    params.setNotInInstitutions(new ArrayList<>(keysFound));
    result = duplicatesService.findPossibleDuplicates(params);
    assertEquals(0, result.getDuplicates().size());
  }

  @Test
  @Execution(ExecutionMode.CONCURRENT)
  public void invalidEmailsTest() {
    Collection collection = new Collection();
    collection.setCode("cc");
    collection.setName("n1");
    collection.setEmail(Collections.singletonList("asfs"));

    assertThrows(ConstraintViolationException.class, () -> collectionService.create(collection));

    collection.setEmail(Collections.singletonList("aa@aa.com"));
    UUID key = collectionService.create(collection);
    Collection collectionCreated = collectionService.get(key);

    collectionCreated.getEmail().add("asfs");
    assertThrows(
        ConstraintViolationException.class, () -> collectionService.update(collectionCreated));
  }
}
