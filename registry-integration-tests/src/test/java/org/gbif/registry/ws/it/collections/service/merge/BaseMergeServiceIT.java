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

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.IdType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.OccurrenceMappeable;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.collections.UserId;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.service.collections.OccurrenceMappingService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.MachineTagService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.NodeType;
import org.gbif.api.vocabulary.ParticipationStatus;
import org.gbif.registry.service.collections.merge.MergeService;
import org.gbif.registry.ws.it.collections.service.BaseServiceIT;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.gbif.registry.domain.collections.Constants.IDIGBIO_NAMESPACE;
import static org.gbif.registry.service.collections.utils.MasterSourceUtils.DATASET_SOURCE;
import static org.gbif.registry.service.collections.utils.MasterSourceUtils.IH_SOURCE;
import static org.gbif.registry.service.collections.utils.MasterSourceUtils.MASTER_SOURCE_COLLECTIONS_NAMESPACE;
import static org.gbif.registry.service.collections.utils.MasterSourceUtils.ORGANIZATION_SOURCE;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class BaseMergeServiceIT<
        T extends
            CollectionEntity & Identifiable & MachineTaggable & OccurrenceMappeable & Contactable
                & Taggable & Commentable>
    extends BaseServiceIT {

  protected final MergeService<T> mergeService;
  protected final CrudService<T> crudService;
  protected final IdentifierService identifierService;
  protected final ContactService contactService;
  protected final MachineTagService machineTagService;
  protected final OccurrenceMappingService occurrenceMappingService;
  protected final PersonService personService;

  @Autowired private DatasetService datasetService;
  @Autowired private NodeService nodeService;
  @Autowired private OrganizationService organizationService;
  @Autowired private InstallationService installationService;

  public BaseMergeServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      MergeService<T> mergeService,
      CrudService<T> crudService,
      IdentifierService identifierService,
      ContactService contactService,
      MachineTagService machineTagService,
      OccurrenceMappingService occurrenceMappingService,
      PersonService personService) {
    super(simplePrincipalProvider);
    this.mergeService = mergeService;
    this.crudService = crudService;
    this.identifierService = identifierService;
    this.contactService = contactService;
    this.machineTagService = machineTagService;
    this.occurrenceMappingService = occurrenceMappingService;
    this.personService = personService;
  }

  @Test
  public void mergeTest() {
    T toReplace = createEntityToReplace();

    // addresses
    Address a1 = new Address();
    a1.setAddress("a1");
    toReplace.setAddress(a1);

    Address ma1 = new Address();
    ma1.setAddress("ma1");
    toReplace.setMailingAddress(ma1);

    crudService.create(toReplace);

    // identifiers
    Identifier identifier = new Identifier(IdentifierType.LSID, "test");
    identifierService.addIdentifier(toReplace.getKey(), identifier);

    // contacts
    Person p1 = new Person();
    p1.setFirstName("p1");
    personService.create(p1);

    Person p2 = new Person();
    p2.setFirstName("p2");
    personService.create(p2);

    contactService.addContact(toReplace.getKey(), p1.getKey());
    contactService.addContact(toReplace.getKey(), p2.getKey());

    // contact persons
    Contact contact1 = new Contact();
    contact1.setFirstName("contact1");
    contact1.setEmail(Collections.singletonList("c1@test.com"));
    contact1.getUserIds().add(new UserId(IdType.OTHER, "12345"));

    Contact contact2 = new Contact();
    contact2.setFirstName("contact2");
    contact2.setEmail(Collections.singletonList("c2@test.com"));
    contact2.getUserIds().add(new UserId(IdType.OTHER, "abcde"));

    contactService.addContactPerson(toReplace.getKey(), contact1);
    contactService.addContactPerson(toReplace.getKey(), contact2);

    // machine tags
    MachineTag mt1 = new MachineTag(IDIGBIO_NAMESPACE, "test", "test");
    machineTagService.addMachineTag(toReplace.getKey(), mt1);
    MachineTag mt2 = new MachineTag("foo", "test", "test");
    machineTagService.addMachineTag(toReplace.getKey(), mt2);

    // occurrence mappings
    Dataset dataset = createDataset();
    OccurrenceMapping om1 = new OccurrenceMapping();
    om1.setDatasetKey(dataset.getKey());
    occurrenceMappingService.addOccurrenceMapping(toReplace.getKey(), om1);

    T replacement = createReplacement();

    Address a2 = new Address();
    a2.setAddress("a2");
    replacement.setAddress(a2);

    crudService.create(replacement);

    contactService.addContact(replacement.getKey(), p1.getKey());

    contact1.setKey(null);
    contactService.addContactPerson(replacement.getKey(), contact1);

    mergeService.merge(toReplace.getKey(), replacement.getKey());

    T replaced = crudService.get(toReplace.getKey());
    T replacementUpdated = crudService.get(replacement.getKey());

    assertEquals(1, replaced.getIdentifiers().size());
    assertNotEquals(toReplace.getModified(), replaced.getModified());
    assertEquals(getSimplePrincipalProvider().get().getName(), replaced.getModifiedBy());
    assertEquals(getSimplePrincipalProvider().get().getName(), replacementUpdated.getModifiedBy());
    assertEquals(2, replacementUpdated.getIdentifiers().size());
    assertEquals(2, replaced.getMachineTags().size());
    assertEquals(1, replacementUpdated.getMachineTags().size());
    assertEquals(2, replacementUpdated.getContacts().size());
    assertEquals(2, replacementUpdated.getContactPersons().size());
    assertTrue(a2.lenientEquals(replacementUpdated.getAddress()));
    assertTrue(ma1.lenientEquals(replacementUpdated.getMailingAddress()));
    assertEquals(replacement.getCreatedBy(), replacementUpdated.getCreatedBy());
    assertNull(replacementUpdated.getDeleted());
    assertEquals(1, replaced.getOccurrenceMappings().size());
    assertEquals(1, replacementUpdated.getOccurrenceMappings().size());

    extraAsserts(replaced, replacement, replacementUpdated);
  }

  @Test
  public void preconditionsTest() {
    T e1 = createEntityToReplace();
    crudService.create(e1);
    MachineTag mt1 = new MachineTag(MASTER_SOURCE_COLLECTIONS_NAMESPACE, IH_SOURCE, "test");
    machineTagService.addMachineTag(e1.getKey(), mt1);

    T e2 = createReplacement();
    crudService.create(e2);
    MachineTag mt2 = new MachineTag(MASTER_SOURCE_COLLECTIONS_NAMESPACE, IH_SOURCE, "test");
    machineTagService.addMachineTag(e2.getKey(), mt2);

    assertThrows(
        IllegalArgumentException.class, () -> mergeService.merge(e1.getKey(), e2.getKey()));

    assertThrows(
        IllegalArgumentException.class, () -> mergeService.merge(e1.getKey(), UUID.randomUUID()));

    assertThrows(
        IllegalArgumentException.class, () -> mergeService.merge(UUID.randomUUID(), e2.getKey()));

    // test that we can't merge 2 idigbio entities
    machineTagService.deleteMachineTag(e1.getKey(), mt1.getKey());
    machineTagService.deleteMachineTag(e2.getKey(), mt2.getKey());

    MachineTag mt3 = new MachineTag(IDIGBIO_NAMESPACE, "foo", "bar");
    machineTagService.addMachineTag(e1.getKey(), mt3);
    MachineTag mt4 = new MachineTag(IDIGBIO_NAMESPACE, "foo2", "bar2");
    machineTagService.addMachineTag(e2.getKey(), mt4);
    assertThrows(
        IllegalArgumentException.class, () -> mergeService.merge(e1.getKey(), e2.getKey()));

    // test that we can't merge 2 entities that have a master source
    machineTagService.deleteMachineTag(e1.getKey(), mt3.getKey());
    machineTagService.deleteMachineTag(e2.getKey(), mt4.getKey());

    String tagName = null;
    if (e1 instanceof Institution) {
      tagName = ORGANIZATION_SOURCE;
    } else {
      tagName = DATASET_SOURCE;
    }
    machineTagService.addMachineTag(
        e1.getKey(),
        new MachineTag(MASTER_SOURCE_COLLECTIONS_NAMESPACE, tagName, UUID.randomUUID().toString()));
    machineTagService.addMachineTag(
        e2.getKey(),
        new MachineTag(MASTER_SOURCE_COLLECTIONS_NAMESPACE, tagName, UUID.randomUUID().toString()));
    assertThrows(
        IllegalArgumentException.class, () -> mergeService.merge(e1.getKey(), e2.getKey()));
  }

  protected Dataset createDataset() {
    Node node = new Node();
    node.setTitle("node");
    node.setType(NodeType.COUNTRY);
    node.setParticipationStatus(ParticipationStatus.AFFILIATE);
    nodeService.create(node);

    Organization org = new Organization();
    org.setEndorsingNodeKey(node.getKey());
    org.setTitle("organization");
    org.setLanguage(Language.ABKHAZIAN);
    org.setPassword("testtttt");
    organizationService.create(org);

    Installation installation = new Installation();
    installation.setTitle("title");
    installation.setOrganizationKey(org.getKey());
    installation.setType(InstallationType.BIOCASE_INSTALLATION);
    installationService.create(installation);

    Dataset dataset = new Dataset();
    dataset.setTitle("title");
    dataset.setInstallationKey(installation.getKey());
    dataset.setPublishingOrganizationKey(org.getKey());
    dataset.setType(DatasetType.CHECKLIST);
    dataset.setLanguage(Language.ABKHAZIAN);
    dataset.setLicense(License.CC0_1_0);
    datasetService.create(dataset);

    return dataset;
  }

  abstract T createEntityToReplace();

  abstract T createReplacement();

  abstract void extraAsserts(T replaced, T replacement, T replacementUpdated);
}
