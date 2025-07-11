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
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.service.collections.merge.InstitutionMergeService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/** Tests the {@link InstitutionMergeService}. */
public class InstitutionMergeServiceIT extends BaseMergeServiceIT<Institution> {

  private final InstitutionMergeService institutionMergeService;
  private final InstitutionService institutionService;
  private final CollectionService collectionService;

  @Autowired
  public InstitutionMergeServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      InstitutionMergeService institutionMergeService,
      InstitutionService institutionService,
      CollectionService collectionService) {
    super(
        simplePrincipalProvider,
        institutionMergeService,
        institutionService,
        institutionService,
        institutionService,
        institutionService,
        institutionService,
        institutionService);
    this.institutionMergeService = institutionMergeService;
    this.institutionService = institutionService;
    this.collectionService = collectionService;
  }

  @Test
  public void convertToCollectionAndCreateNewInstitutionTest() {
    Institution toConvert = new Institution();
    toConvert.setCode("tco");
    toConvert.setName("to convert");
    toConvert.setDescription("desc");
    institutionService.create(toConvert);

    // identifiers
    identifierService.addIdentifier(
        toConvert.getKey(), new Identifier(IdentifierType.LSID, "test"));

    // contact persons
    Contact contact1 = new Contact();
    contact1.setFirstName("contact1");
    contact1.setEmail(Collections.singletonList("c1@test.com"));
    contactService.addContactPerson(toConvert.getKey(), contact1);

    // machine tags
    machineTagService.addMachineTag(toConvert.getKey(), new MachineTag("test", "test", "test"));

    // occurrence mappings
    Dataset dataset = createDataset();
    OccurrenceMapping om1 = new OccurrenceMapping();
    om1.setDatasetKey(dataset.getKey());
    occurrenceMappingService.addOccurrenceMapping(toConvert.getKey(), om1);

    // collections
    Collection c1 = new Collection();
    c1.setName("coll");
    c1.setCode("c1");
    c1.setInstitutionKey(toConvert.getKey());
    collectionService.create(c1);

    final String newInstitutionName = "new institution";
    UUID newCollectionKey =
        institutionMergeService.convertToCollection(toConvert.getKey(), null, newInstitutionName);

    Institution converted = institutionService.get(toConvert.getKey());
    assertNotNull(converted.getDeleted());
    assertEquals(newCollectionKey, converted.getConvertedToCollection());

    Collection newCollection = collectionService.get(newCollectionKey);
    assertNotNull(newCollection.getInstitutionKey());
    assertEquals(toConvert.getDescription(), newCollection.getDescription());
    assertNotEquals(toConvert.getKey(), newCollection.getInstitutionKey());

    Institution newInstitution = institutionService.get(newCollection.getInstitutionKey());
    assertEquals(newCollection.getCode(), newInstitution.getCode());
    assertEquals(newInstitutionName, newInstitution.getName());

    Collection c1Updated = collectionService.get(c1.getKey());
    assertEquals(newInstitution.getKey(), c1Updated.getInstitutionKey());

    assertEquals(1, newCollection.getIdentifiers().size());
    assertEquals(1, newCollection.getMachineTags().size());
    assertEquals(1, newCollection.getOccurrenceMappings().size());
    assertEquals(1, newCollection.getContactPersons().size());
  }

  @Test
  public void convertToCollectionWithExistingInstitutionTest() {
    Institution toConvert = new Institution();
    toConvert.setCode("tco");
    toConvert.setName("to convert");
    toConvert.setDescription("desc");
    toConvert.setActive(true);
    institutionService.create(toConvert);

    Institution another = new Institution();
    another.setCode("a");
    another.setName("another");
    institutionService.create(another);

    UUID newCollectionKey =
        institutionMergeService.convertToCollection(toConvert.getKey(), another.getKey(), null);

    Institution converted = institutionService.get(toConvert.getKey());
    assertNotNull(converted.getDeleted());
    assertEquals(newCollectionKey, converted.getConvertedToCollection());

    Collection newCollection = collectionService.get(newCollectionKey);
    assertEquals(another.getKey(), newCollection.getInstitutionKey());
    assertTrue(newCollection.isActive());
  }

  @Test
  public void convertToCollectionMissingArgsTest() {
    assertThrows(
        IllegalArgumentException.class,
        () -> institutionMergeService.convertToCollection(UUID.randomUUID(), null, null));
  }

  @Override
  Institution createEntityToReplace() {
    Institution toReplace = new Institution();
    toReplace.setCode("c1");
    toReplace.setName("n1");
    toReplace.setDescription("description");
    toReplace.getEmail().add("a@a.com");
    toReplace.getEmail().add("b@a.com");
    return toReplace;
  }

  @Override
  Institution createReplacement() {
    Institution replacement = new Institution();
    replacement.setCode("c2");
    replacement.setName("n2");
    replacement.getEmail().add("a@a.com");
    return replacement;
  }

  @Override
  void extraAsserts(Institution replaced, Institution replacement, Institution replacementUpdated) {
    assertEquals(replacementUpdated.getKey(), replaced.getReplacedBy());
    assertEquals(2, replacementUpdated.getEmail().size());
    assertEquals(replaced.getDescription(), replacementUpdated.getDescription());
  }
}
